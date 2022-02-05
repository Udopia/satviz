package edu.kit.satviz.consumer.bindings;

import java.lang.invoke.VarHandle;
import jdk.incubator.foreign.CLinker;
import jdk.incubator.foreign.MemoryLayout;
import jdk.incubator.foreign.MemoryLayout.PathElement;
import jdk.incubator.foreign.MemorySegment;

public final class NodeInfo extends NativeObject {

  public static final MemoryLayout LAYOUT = withPadding(
      CLinker.C_INT.withName("index"),
      CLinker.C_INT.withName("heat"),
      CLinker.C_FLOAT.withName("x"),
      CLinker.C_FLOAT.withName("y")
  );

  private static final VarHandle INDEX_HANDLE = LAYOUT.varHandle(int.class,
      PathElement.groupElement("index"));
  private static final VarHandle HEAT_HANDLE = LAYOUT.varHandle(int.class,
      PathElement.groupElement("heat"));
  private static final VarHandle X_HANDLE = LAYOUT.varHandle(float.class,
      PathElement.groupElement("x"));
  private static final VarHandle Y_HANDLE = LAYOUT.varHandle(float.class,
      PathElement.groupElement("y"));

  private final int index;
  private final int heat;
  private final float x;
  private final float y;

  public NodeInfo(MemorySegment segment) {
    super(segment.address());
    this.index = (int) INDEX_HANDLE.get(segment);
    this.heat = (int) HEAT_HANDLE.get(segment);
    this.x = (float) X_HANDLE.get(segment);
    this.y = (float) Y_HANDLE.get(segment);
  }

  public int getIndex() {
    return index;
  }

  public int getHeat() {
    return heat;
  }

  public float getX() {
    return x;
  }

  public float getY() {
    return y;
  }

  @Override
  public void close() {
    CLinker.freeMemory(pointer);
  }
}
