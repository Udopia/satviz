package edu.kit.satviz.sat;

import java.util.Arrays;

/**
 * This class holds a truth assignment for the variables of a SAT-instance.
 *
 * @author quorty, luwae
 */
public class SatAssignment {

  /**
   * This enum represents the variable state.
   */
  public enum VariableState {
    DONTCARE,
    SET,
    UNSET,
    RESERVED;

    /**
     * This method converts a given integer state representation into an instance of the
     * <code>VariableState</code> enum.
     *
     * @param intState Integer representation of the variable state.
     * @return An instance of the <code>VariableState</code> enum.
     */
    public static VariableState fromIntState(int intState) {
      return switch ((int) Math.signum(intState)) {
        case 0 -> DONTCARE;
        case -1 -> UNSET;
        case 1 -> SET;
        default -> null;
      };
    }

  }

  private final int varCount;
  private final VariableState[] variableStates;

  /**
   * An instance of the SatAssignment class always starts out
   * with every variable state being <code>DONTCARE</code>.<br>
   *
   * <p>
   * <i>NOTE: The variable index starts with</i> <code>1</code> <i>instead of the usual</i>
   * <code>0</code>. <code>varCount</code> <i>has to be greater than</i> <code>0</code>.
   * </p>
   *
   * @param varCount The total amount of different variables.
   * @throws IllegalArgumentException In case <code>varCount ≤ 0</code>.
   */
  public SatAssignment(int varCount) {
    if (varCount <= 0) {
      throw new IllegalArgumentException();
    }
    this.varCount = varCount;
    this.variableStates = new VariableState[varCount];
    for (int i = 0; i < varCount; i++) {
      this.variableStates[i] = VariableState.DONTCARE;
    }
  }

  /**
   * This method sets the state of a variable.<br>
   *
   * <p>
   * <i>NOTE: In case an invalid variable or state is entered, nothing changes.</i>
   * </p>
   *
   * @param variable The variable, whose state is being set.
   * @param state    The state as an instance of the <code>VariableState</code> class.
   * @throws IllegalArgumentException In case <code>variable ≤ 0</code>,
   *                                  <code>variable > varCount</code> or
   *                                  <code>state == null</code>.
   */
  public void set(int variable, VariableState state) {
    if (variable <= 0 || variable > this.varCount || state == null) {
      throw new IllegalArgumentException();
    }
    this.variableStates[variable - 1] = state;
  }

  /**
   * This method gets the state of a variable as a <code>VariableState</code>.<br>
   *
   * <p>
   * <i>NOTE: In case an invalid variable is entered,</i>
   * <code>DONTCARE</code> <i>is returned.</i>
   * </p>
   *
   * @param variable The variable, whose state is being returned.
   * @return The state as an instance of the <code>VariableState</code> class.
   * @throws IllegalArgumentException In case <code>variable ≤ 0</code> or
   *                                  <code>variable > varCount</code>.
   */
  public VariableState get(int variable) {
    if (variable <= 0 || variable > this.varCount) {
      throw new IllegalArgumentException();
    }
    return this.variableStates[variable - 1];
  }

  /**
   * This method gets the integer representation of the state of a variable.<br>
   *
   * <p>
   * <i>NOTE: In case an invalid variable is entered,</i>
   * <code>0</code> <i>is returned.</i>
   * </p>
   *
   * @param variable The variable, whose state is being returned.
   * @return The state as an integer value, that can also hold the variable-ID.
   * @throws IllegalArgumentException In case <code>variable ≤ 0</code> or
   *                                  <code>variable > varCount</code>.
   */
  public int getIntState(int variable) {
    return convertVariableStateToIntState(variable, get(variable));
  }

  /**
   * This method converts an instance of the <code>VariableState</code> class
   * into an integer representation of the state.
   *
   * <p>
   * <i>NOTE: In case an invalid state is entered,</i>
   * <code>0</code> <i>is returned.</i>
   * </p>
   *
   * @param variable The variable, whose state is being returned.
   * @param state    The state as an instance of the <code>VariableState</code> class.
   * @return The state as an integer value, that can also hold the variable-ID.
   * @throws IllegalArgumentException In case <code>state == null</code>.
   */
  public static int convertVariableStateToIntState(int variable, VariableState state) {
    if (state == null) {
      throw new IllegalArgumentException();
    }
    return switch (state) {
      case SET -> variable;
      case UNSET -> -variable;
      default -> 0;
    };
  }

  /**
   * This simple getter-method returns the total amount of variables.
   *
   * @return The total amount of variables.
   */
  public int getVarCount() {
    return this.varCount;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SatAssignment satAssignment = (SatAssignment) o;
    if (this.varCount != satAssignment.getVarCount()) {
      return false;
    }
    for (int i = 1; i <= this.varCount; i++) {
      if (this.variableStates[i - 1].equals(satAssignment.get(i))) {
        return false;
      }
    }
    return true;
  }

  @Override
  public int hashCode() {
    return Arrays.hashCode(this.variableStates);
  }

  @Override
  public String toString() {
    return Arrays.toString(this.variableStates);
  }

}
