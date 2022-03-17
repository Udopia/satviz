package edu.kit.satviz.consumer;

import edu.kit.satviz.common.Hashing;
import edu.kit.satviz.consumer.config.ConsumerConfig;
import edu.kit.satviz.consumer.config.ConsumerMode;
import edu.kit.satviz.consumer.config.ConsumerModeConfig;
import edu.kit.satviz.consumer.config.EmbeddedModeConfig;
import edu.kit.satviz.consumer.config.ExternalModeConfig;
import edu.kit.satviz.consumer.display.DisplayType;
import edu.kit.satviz.consumer.display.VideoController;
import edu.kit.satviz.consumer.graph.Graph;
import edu.kit.satviz.consumer.graph.HeatUpdate;
import edu.kit.satviz.consumer.graph.WeightUpdate;
import edu.kit.satviz.consumer.gui.GuiUtils;
import edu.kit.satviz.consumer.gui.config.ConfigStarter;
import edu.kit.satviz.consumer.gui.visualization.VisualizationController;
import edu.kit.satviz.consumer.gui.visualization.VisualizationStarter;
import edu.kit.satviz.consumer.processing.ClauseCoordinator;
import edu.kit.satviz.consumer.processing.Mediator;
import edu.kit.satviz.consumer.processing.RecencyHeatmap;
import edu.kit.satviz.consumer.processing.RingInteractionGraph;
import edu.kit.satviz.consumer.processing.VariableInteractionGraph;
import edu.kit.satviz.network.pub.ConsumerConnection;
import edu.kit.satviz.network.pub.OfferType;
import edu.kit.satviz.network.pub.ProducerId;
import edu.kit.satviz.network.pub.SolverId;
import edu.kit.satviz.parsers.DimacsFile;
import edu.kit.satviz.parsers.ParsingException;
import edu.kit.satviz.sat.ClauseUpdate;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.StreamSupport;
import net.lingala.zip4j.ZipFile;

public final class ConsumerApplication {

  private static final Logger logger = Logger.getLogger("Consumer");
  private static ProducerId pid = null;
  private static final Object SYNC_OBJECT = new Object();

  public static void main(String[] args) throws IOException, InterruptedException, ExecutionException {
    /*Graph g = Graph.create(1040);
    VideoController c = VideoController.create(g, DisplayType.ONSCREEN, 1000, 700);
    new Thread(() -> {
      HeatUpdate u = new HeatUpdate();
      u.add(1025, 1);
      g.submitUpdate(u);
    }).start();
    if (true) {
      return;
    }*/
    logger.setLevel(Level.FINER);
    logger.log(Level.FINER, "Starting consumer with arguments {0}", args);
    ConsumerConfig config = getStartingConfig(args);
    if (config == null) {
      return;
    }
    Path tempDir = Files.createTempDirectory("satviz");
    tempDir.toFile().deleteOnExit();
    logger.info("Setting up network connection");
    ConsumerConnection connection = setupNetworkConnection(config, tempDir);
    logger.log(Level.INFO, "Producer {0} connected", pid);
    if (pid.getType() == OfferType.SOLVER) {
      SolverId sid = (SolverId) pid;
      long hash = Hashing.hashContent(Files.newInputStream(config.getInstancePath()));
      if (hash != sid.getInstanceHash()) {
        logger.log(Level.SEVERE, "SAT instance mismatch: {0} (local) vs {1} (remote)",
            new Object[] { hash, sid.getInstanceHash() });
        connection.disconnect(pid);
        System.exit(1);
        return;
      }
    }

    int variableAmount;
    logger.finer("Reading SAT instance file");
    VariableInteractionGraph vig = new RingInteractionGraph(config.getWeightFactor());
    WeightUpdate initialUpdate;
    try (DimacsFile dimacsFile = new DimacsFile(Files.newInputStream(config.getInstancePath()))) {
      variableAmount = dimacsFile.getVariableAmount();
      logger.log(Level.INFO, "Instance contains {0} variables", variableAmount);
      ClauseUpdate[] clauses = StreamSupport.stream(dimacsFile.spliterator(), false)
          .toArray(ClauseUpdate[]::new);
      initialUpdate = vig.process(clauses, null);
    } catch (ParsingException e) {
      if (!config.isNoGui()) {
        // Error window.
      }
      throw e;
    }

    ScheduledExecutorService glScheduler = Executors.newSingleThreadScheduledExecutor();

    record GlComponents(Graph graph, VideoController controller) {}

    GlComponents components = glScheduler.submit(() -> {
      Graph graph = Graph.create(variableAmount);
      VideoController videoController = VideoController.create(
          graph,
          (config.isNoGui()) ? DisplayType.OFFSCREEN : DisplayType.ONSCREEN,
          1920,
          1080
      );
      return new GlComponents(graph, videoController);
    }).get();

    logger.finer("Initialising OpenGL window");

    logger.info("Calculating initial layout");
    glScheduler.submit(() -> {
      //System.out.println(initialUpdate);
      try {
        //WeightUpdate update = new WeightUpdate();
        //update.add(256, 257, 1);
        HeatUpdate u = new HeatUpdate();
        for (int i = 0; i < variableAmount; i++) {
          u.add(i, 0);
        }
        components.graph.submitUpdate(u);
        components.graph.submitUpdate(initialUpdate);
        components.graph.recalculateLayout();
        components.controller.nextFrame();
      } catch (Throwable e) {
        e.printStackTrace();
      }
    }).get();

    ClauseCoordinator coordinator = new ClauseCoordinator(components.graph,
        tempDir, variableAmount);

    Mediator mediator = new Mediator.MediatorBuilder()
        .setConfig(config)
        .setGlScheduler(glScheduler)
        .setController(components.controller)
        .setGraph(components.graph)
        .setCoordinator(coordinator)
        .setHeatmap(new RecencyHeatmap(config.getWindowSize()))
        .setVig(vig)
        .setConnection(connection)
        .createMediator();

    if (!config.isNoGui()) {
      VisualizationController visController = new VisualizationController(mediator, config, variableAmount);
      coordinator.registerChangeListener(visController::onClauseUpdate);

      // TODO: 21/02/2022 add back in
      VisualizationStarter.setVisualizationController(visController);
      GuiUtils.launch(VisualizationStarter.class);
      //Application.launch(VisualizationStarter.class);
    }

    connection.connect(ConsumerApplication.pid, mediator);

    if (config.isRecordImmediately() || config.isNoGui()) {
      mediator.startOrStopRecording();
    }

    mediator.startRendering();



    //coordinator.close();
    //videoController.close();
  }

  private static ConsumerConfig getStartingConfig(String[] args) throws InterruptedException {
    if (args.length == 0) {
      GuiUtils.launch(ConfigStarter.class);
      synchronized (GuiUtils.CONFIG_MONITOR) {
        while (!ConfigStarter.isDone()) {
          GuiUtils.CONFIG_MONITOR.wait();
        }
      }
      return ConfigStarter.getConsumerConfig();
    } else {
      // TODO: parse Arguments from CLI
      return null;
    }
  }

  private static ConsumerConnection setupNetworkConnection(ConsumerConfig config, Path tempDir)
      throws InterruptedException {
    ConsumerModeConfig modeConfig = config.getModeConfig();
    boolean embedded = modeConfig.getMode() == ConsumerMode.EMBEDDED;
    int consumerPort = embedded ? 0 : ((ExternalModeConfig) modeConfig).getPort();
    ConsumerConnection connection = new ConsumerConnection(consumerPort,
        ConsumerApplication::newConnectionAvailable, (s) -> logger.log(Level.SEVERE,
        "network fail: {0}", s));
    try {
      connection.start();
    } catch (IOException e) {
      logger.log(Level.SEVERE, "Error while trying to start embedded producer", e);
      System.exit(1);
      return null;
    }
    try {
      logger.log(Level.INFO, "Port {0} opened", String.valueOf(connection.getPort()));
    } catch (IOException e) {
      logger.log(Level.SEVERE, "Error while trying to start embedded producer", e);
      System.exit(1);
      return null;
    }

    if (embedded) {
      logger.info("Starting embedded producer");
      EmbeddedModeConfig embedConfig = (EmbeddedModeConfig) modeConfig;
      try {
        String sourcePath = embedConfig.getSourcePath().toString();
        List<String> baseArgs = List.of("-H", InetAddress.getLocalHost().getHostAddress(),
            "-P", String.valueOf(connection.getPort()));
        List<String> additionalArgs = switch (embedConfig.getSource()) {
          case SOLVER -> List.of("-s", sourcePath, "-i", config.getInstancePath().toString());
          case PROOF -> List.of("-p", sourcePath);
        };
        List<String> allArgs = new ArrayList<>();
        allArgs.add("sh");
        allArgs.add("./sat-prod");
        allArgs.addAll(baseArgs);
        allArgs.addAll(additionalArgs);
        Path bin = extractProducer(tempDir);
        new ProcessBuilder()
            .directory(bin.toFile())
            .command(allArgs)
            .inheritIO()
            .start();
      } catch (IOException e) {
        logger.log(Level.SEVERE, "Error while trying to start embedded producer", e);
        System.exit(1);
        return null;
      }
    }
    logger.info("Waiting for producer to connect...");
    synchronized (SYNC_OBJECT) {
      while (ConsumerApplication.pid == null) {
        SYNC_OBJECT.wait();
      }
    }
    return connection;
  }

  private static Path extractProducer(Path tempDir) throws IOException {
    Path producerDir = Files.createTempDirectory(tempDir, "producer");
    try (var producerStream = ConsumerApplication.class.getResourceAsStream("/satviz-producer.zip")) {
      Path producerZip = producerDir.resolve("producer.zip");
      Files.copy(producerStream, producerZip);
      try (ZipFile zip = new ZipFile(producerZip.toFile())) {
        zip.extractAll(producerDir.toString());
      }
    }
    return producerDir.resolve("satviz-producer/bin/");
  }

  private static void newConnectionAvailable(ProducerId pid) {
    synchronized (SYNC_OBJECT) {
      if (ConsumerApplication.pid == null) {
        ConsumerApplication.pid = pid;
        SYNC_OBJECT.notifyAll();
      }
    }
  }

}
