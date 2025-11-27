package common;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import java.io.EOFException;
import java.io.IOException;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class SerializationUtils {
  private static final ObjectMapper mapper;

  static {
    PolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder()
            .allowIfSubType("common.commands")
            .allowIfSubType("common.dto")
            // NEW: This is the critical fix. We must allow our CaseFile DTO to be serialized.
            .allowIfSubType("JsonDTO")
            .allowIfSubType(java.util.List.class)
            .allowIfSubType(java.util.Map.class)
            .build();

    mapper = new ObjectMapper();
    mapper.activateDefaultTyping(ptv, ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY);
  }

  private SerializationUtils() {}

  public static byte[] serialize(Serializable object) throws IOException {
    return mapper.writeValueAsBytes(object);
  }

  public static Object deserialize(byte[] bytes) throws IOException {
    return mapper.readValue(bytes, Object.class);
  }

  public static void writeFramedObject(SocketChannel channel, Serializable object) throws IOException {
    byte[] objectBytes = serialize(object);
    int length = objectBytes.length;
    ByteBuffer buffer = ByteBuffer.allocate(4 + length);
    buffer.putInt(length);
    buffer.put(objectBytes);
    buffer.flip();

    while (buffer.hasRemaining()) {
      channel.write(buffer);
    }
  }

  public static Object readFramedObject(SocketChannel channel) throws IOException {
    ByteBuffer lengthBuffer = ByteBuffer.allocate(4);
    int bytesRead = 0;
    while (bytesRead < 4) {
      int read = channel.read(lengthBuffer);
      if (read == -1) {
        if (bytesRead == 0) return null;
        throw new EOFException("Stream ended prematurely while reading object length.");
      }
      bytesRead += read;
    }

    lengthBuffer.flip();
    int objectLength = lengthBuffer.getInt();

    if (objectLength <= 0 || objectLength > 10 * 1024 * 1024) { // Max 10MB
      throw new IOException("Invalid object length received: " + objectLength);
    }

    ByteBuffer objectBuffer = ByteBuffer.allocate(objectLength);
    bytesRead = 0;
    while (bytesRead < objectLength) {
      int read = channel.read(objectBuffer);
      if (read == -1) {
        throw new EOFException("Stream ended prematurely while reading object data.");
      }
      bytesRead += read;
    }

    return deserialize(objectBuffer.array());
  }
}