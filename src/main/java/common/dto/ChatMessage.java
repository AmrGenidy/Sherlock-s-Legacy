package common.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serial;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;

public class ChatMessage implements Serializable {
  @Serial
  private static final long serialVersionUID = 1L;
  private final String senderDisplayId;
  private final String text;
  private final long timestamp;

  @JsonCreator
  public ChatMessage(
          @JsonProperty("senderDisplayId") String senderDisplayId,
          @JsonProperty("text") String text,
          @JsonProperty("timestamp") long timestamp) {
    this.senderDisplayId = Objects.requireNonNull(senderDisplayId);
    this.text = Objects.requireNonNull(text);
    this.timestamp = timestamp;
  }

  public String getSenderDisplayId() {
    return senderDisplayId;
  }

  public String getText() {
    return text;
  }

  public long getTimestamp() {
    return timestamp;
  }

  @Override
  public String toString() {
    SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
    return "[" + sdf.format(new Date(timestamp)) + "] " + senderDisplayId + ": " + text;
  }
}