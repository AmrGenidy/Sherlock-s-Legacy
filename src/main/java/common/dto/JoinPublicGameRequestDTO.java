package common.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

public class JoinPublicGameRequestDTO implements Serializable {
  @Serial
  private static final long serialVersionUID = 1L;
  private final String sessionId;

  @JsonCreator
  public JoinPublicGameRequestDTO(@JsonProperty("sessionId") String sessionId) {
    this.sessionId = Objects.requireNonNull(sessionId);
  }

  public String getSessionId() {
    return sessionId;
  }
}