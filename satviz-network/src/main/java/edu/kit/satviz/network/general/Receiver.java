package edu.kit.satviz.network.general;

import edu.kit.satviz.serial.SerialBuilder;
import edu.kit.satviz.serial.SerializationException;
import java.nio.ByteBuffer;
import java.util.function.IntFunction;

/**
 * Transforms a byte stream into a network message stream.
 * Messages are deserialized based on given {@link SerialBuilder}s.
 */
public class Receiver {
  private final IntFunction<SerialBuilder<?>> gen;
  private byte type;
  private SerialBuilder<?> builder = null;

  private boolean failed = false;

  /**
   * Creates a new empty receiver.
   *
   * @param gen a function generating {@link SerialBuilder}s from a type
   */
  public Receiver(IntFunction<SerialBuilder<?>> gen) {
    this.gen = gen;
  }

  /**
   * Receives input bytes from a bytebuffer and constructs messages.
   * The bytes previously read are taken into account, so that a long stream
   *     of bytes can be received by subsequent calls to <code>receive</code>.
   * Reads bytes as long as the buffer is not empty and an object is not finished.
   *
   * @param bb the buffer to read from
   * @return a message if one was received in its entirety, <code>null</code> otherwise
   * @throws SerializationException if the deserialization fails or has failed previously
   */
  public NetworkMessage receive(ByteBuffer bb) throws SerializationException {
    if (failed) {
      throw new SerializationException("serialization failed previously");
    }

    int nb = bb.remaining();
    if (nb == 0) {
      return null;
    }

    if (builder == null) {
      type = bb.get();
      nb--;
      builder = gen.apply(type); // get new builder according to type
      if (builder == null) { // didn't get one
        failed = true;
        throw new SerializationException("no builder available for this type: " + type);
      }
    }

    while (nb > 0) {
      nb--;
      boolean done;
      try {
        done = builder.addByte(bb.get());
      } catch (SerializationException e) {
        failed = true;
        throw e;
      }

      if (done) {
        NetworkMessage msg = new NetworkMessage(type, builder.getObject());
        builder = null; // remove last builder
        return msg;
      }
    }
    return null;
  }
}
