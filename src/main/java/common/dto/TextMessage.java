package common.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serial;
import java.io.Serializable;

public class TextMessage implements Serializable {
  @Serial
  private static final long serialVersionUID = 1L;
  private final String text;
  private final boolean isError;

  @JsonCreator
  public TextMessage(
          @JsonProperty("text") String text,
          @JsonProperty("error") boolean isError) {
    this.text = text;
    this.isError = isError;
  }

  public String getText() {
    return text;
  }

  public boolean isError() {
    return isError;
  }

  @Override
  public String toString() {
    return (isError ? "[ERROR] " : "") + text;
  }
}