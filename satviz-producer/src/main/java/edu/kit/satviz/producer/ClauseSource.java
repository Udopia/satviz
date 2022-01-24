package edu.kit.satviz.producer;

import edu.kit.satviz.sat.Clause;
import edu.kit.satviz.sat.ClauseUpdate;
import edu.kit.satviz.sat.SatAssignment;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public abstract class ClauseSource implements AutoCloseable {

  private final List<Consumer<? super ClauseUpdate>> clauseListeners;

  private Consumer<SatAssignment> doneListener;
  private Runnable closeListener;

  protected ClauseSource() {
    clauseListeners = new ArrayList<>();
    doneListener = a -> {};
    closeListener = () -> {};
  }

  public abstract void open() throws SourceOpeningException;

  public void subscribe(Consumer<? super ClauseUpdate> listener) {
    clauseListeners.add(Objects.requireNonNull(listener));
  }

  public void whenClosed(Runnable action) {
    closeListener = Objects.requireNonNull(action);
  }

  public void whenDone(Consumer<SatAssignment> action) {
    doneListener = Objects.requireNonNull(action);
  }

  protected void notifyListeners(ClauseUpdate update) {
    clauseListeners.forEach(listener -> listener.accept(update));
  }

}
