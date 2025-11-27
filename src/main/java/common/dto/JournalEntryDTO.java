package common.dto;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serial;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;
public class JournalEntryDTO implements Serializable {
  @Serial
  private static final long serialVersionUID = 1L;
  private final String text;
  private final String contributorPlayerId;
  private final long timestamp;

  @JsonCreator
  public JournalEntryDTO(
          @JsonProperty("text") String text,
          @JsonProperty("contributorPlayerId") String contributorPlayerId,
          @JsonProperty("timestamp") long timestamp) {
    this.text = Objects.requireNonNull(text, "Text cannot be null");
    this.contributorPlayerId = Objects.requireNonNull(contributorPlayerId, "Contributor ID cannot be null");
    this.timestamp = timestamp;
  }

  public String getText() {
    return text;
  }

  public String getContributorPlayerId() {
    return contributorPlayerId;
  }

  public long getTimestamp() {
    return timestamp;
  }

  @Override
  public String toString() {
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    String prefix = contributorPlayerId.startsWith("Player") ? contributorPlayerId + ":" : contributorPlayerId;
    return "[" + sdf.format(new Date(timestamp)) + "] " + prefix + " " + text;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    JournalEntryDTO that = (JournalEntryDTO) o;
    return Objects.equals(text, that.text) && Objects.equals(contributorPlayerId, that.contributorPlayerId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(text, contributorPlayerId);
  }
}