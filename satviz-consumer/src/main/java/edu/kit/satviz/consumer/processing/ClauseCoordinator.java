package edu.kit.satviz.consumer.processing;

import edu.kit.satviz.consumer.graph.Graph;
import edu.kit.satviz.sat.ClauseUpdate;
import edu.kit.satviz.serial.SerializationException;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ClauseCoordinator implements AutoCloseable {

  private final Path tempDir;
  private final Path snapshotDir;
  private final TreeMap<Long, Snapshot> snapshots;
  private final List<ClauseUpdateProcessor> processors;
  private final Graph graph;
  private final ExternalClauseBuffer buffer;

  // snapshotLock provides mutual exclusion for snapshot creation and loading
  private final ReentrantLock snapshotLock;
  // stateLock provides mutual exclusion and consistency for operations that modify currentUpdate
  private final ReentrantLock stateLock;
  // processorLock provides mutual exclusion for addProcessor and takeSnapshot to coordinate
  // change detection in the list of processors. it is not needed to access the list elsewhere.
  private final Lock processorLock;

  // currentUpdate is volatile, even though the stateLock prevents concurrent modification already.
  // this is because while updates to currentUpdate need to be consistent and coordinated,
  // concurrent reads are legal. to ensure a consistent and up-to-date view of currentUpdate, it is
  // therefore marked volatile.
  private volatile long currentUpdate;
  private Runnable changeListener;
  private boolean processorsChanged;

  public ClauseCoordinator(Graph graph, Path tempDir) throws IOException {
    this.tempDir = tempDir;
    this.snapshotDir = Files.createTempDirectory(tempDir, "satviz-snapshots");
    this.snapshots = new TreeMap<>();
    this.processors = new CopyOnWriteArrayList<>();
    this.changeListener = () -> {
    };
    this.currentUpdate = 0;
    this.graph = graph;
    this.buffer = new ExternalClauseBuffer(tempDir);
    this.snapshotLock = new ReentrantLock();
    this.stateLock = new ReentrantLock();
    this.processorLock = new ReentrantLock();
    this.processorsChanged = true; // set to true so the first snapshot has something to save
    // take initial snapshot to have a baseline in the snapshots TreeMap
    takeSnapshot();
  }

  public void addProcessor(ClauseUpdateProcessor processor) {
    processorLock.lock();
    try {
      processors.add(processor);
      processorsChanged = true;
    } finally {
      processorLock.unlock();
    }
  }

  public void advanceVisualization(int numUpdates)
      throws IOException, SerializationException {
    // to prevent alien call processor.process from looping back
    if (stateLock.isHeldByCurrentThread()) {
      return;
    }
    advance(numUpdates);
  }

  public long currentUpdate() {
    return currentUpdate;
  }

  public long totalUpdateCount() {
    return buffer.size();
  }

  public void seekToUpdate(long index) throws IOException, SerializationException {
    if (index < 0) {
      throw new IllegalArgumentException("Index must not be negative: " + index);
    }
    // to prevent alien calls from looping back
    if (stateLock.isHeldByCurrentThread()) {
      return;
    }

    stateLock.lock();
    try {
      long closestSnapshotIndex = loadClosestSnapshot(index);
      currentUpdate = closestSnapshotIndex;
      advance((int) (index - closestSnapshotIndex));
    } finally {
      stateLock.unlock();
    }
  }

  private void advance(int numUpdates) throws SerializationException, IOException {
    if (numUpdates < 1) {
      return;
    }

    // the state needs to be locked to synchronise advancements.
    // snapshots need to be locked to prevent snapshot creation of half-updated graphs
    snapshotLock.lock();
    stateLock.lock();
    try {
      ClauseUpdate[] updates = buffer.getClauseUpdates(currentUpdate, numUpdates);
      for (ClauseUpdateProcessor processor : processors) {
        processor.process(updates, graph);
      }
      // this operation is not atomic although currentUpdate is volatile.
      // However, this is no problem because write access to currentUpdate is always coordinated
      // using stateLock.
      currentUpdate += updates.length;
    } finally {
      stateLock.unlock();
      snapshotLock.unlock();
    }
    // the change listener may run concurrently again
    changeListener.run();
  }

  public void takeSnapshot() throws IOException {
    // prevent alien calls from looping back
    if (snapshotLock.isHeldByCurrentThread()) {
      return;
    }

    // snapshots are locked to prevent overlapping snapshot creation and to synchronise access
    // to the snapshots TreeMap
    snapshotLock.lock();
    // processors are locked so that updates happen either before or after taking the snapshot
    processorLock.lock();
    try {
      long current = currentUpdate();
      Path snapshotFile = Files.createTempFile(snapshotDir, "snapshot", null);
      try (var stream = new BufferedOutputStream(Files.newOutputStream(snapshotFile))) {
        for (ClauseUpdateProcessor processor : processors) {
          processor.serialize(stream);
        }
        graph.serialize(stream);
      }

      // if the processor list has changed since the last snapshot, create a new snapshot array.
      // otherwise, reuse the processors snapshot from the most recent snapshot
      ClauseUpdateProcessor[] processorsSnapshot = processorsChanged
          ? this.processors.toArray(new ClauseUpdateProcessor[0])
          : snapshots.floorEntry(current).getValue().processors();
      processorsChanged = false;
      snapshots.put(current, new Snapshot(snapshotFile, processorsSnapshot));
    } finally {
      processorLock.unlock();
      snapshotLock.unlock();
    }

  }

  public void addClauseUpdate(ClauseUpdate clauseUpdate) throws IOException {
    buffer.addClauseUpdate(clauseUpdate);
  }

  public void registerChangeListener(Runnable action) {
    changeListener = Objects.requireNonNull(action);
  }

  private long loadClosestSnapshot(long index) throws IOException {
    // snapshots need to be locked - we don't want to create a snapshot while in the middle of
    // restoring some previous state.
    snapshotLock.lock();
    try {
      // find nearest snapshot to index
      Map.Entry<Long, Snapshot> entry = snapshots.floorEntry(index);
      Snapshot snapshot = entry.getValue();
      List<ClauseUpdateProcessor> snapshotProcessors = Arrays.asList(snapshot.processors());

      try (var stream = new BufferedInputStream(Files.newInputStream(snapshot.file()))) {
        // lock state to ensure consistent graph and processor views for advance()
        stateLock.lock();
        // restore processor and graph state
        for (ClauseUpdateProcessor processor : snapshotProcessors) {
          processor.deserialize(stream);
        }
        graph.deserialize(stream);
      }

      // lock processors so processor updates don't interleave
      processorLock.lock();
      List<ClauseUpdateProcessor> nonSnapshotProcessors = new ArrayList<>(this.processors);
      nonSnapshotProcessors.removeAll(snapshotProcessors);

      // reset processors that were added later
      for (ClauseUpdateProcessor processor : nonSnapshotProcessors) {
        processor.reset();
      }

      List<ClauseUpdateProcessor> newProcessors = new ArrayList<>();
      newProcessors.addAll(snapshotProcessors); // first the processors that exist in the snapshot
      newProcessors.addAll(nonSnapshotProcessors); // then the processors that were added afterwards

      // set new processor list
      this.processors.clear();
      this.processors.addAll(newProcessors);
      processorsChanged = false;
      return entry.getKey();
    } finally {
      stateLock.unlock();
      processorLock.unlock();
      snapshotLock.unlock();
    }
  }

  @Override
  public void close() throws IOException {
    buffer.close();
    // delete tempDir
    Files.walkFileTree(tempDir, new SimpleFileVisitor<>() {
      @Override
      public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        Files.delete(file);
        return FileVisitResult.CONTINUE;
      }

      @Override
      public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
        if (exc != null) {
          throw exc;
        }
        Files.delete(dir);
        return FileVisitResult.CONTINUE;
      }
    });
  }

  private record Snapshot(Path file, ClauseUpdateProcessor[] processors) {

  }

}
