package common.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RoomDescriptionDTO implements Serializable {
  @Serial
  private static final long serialVersionUID = 1L;
  private final String name;
  private final String description;
  private final List<String> objectNames;
  private final List<String> occupantNames;
  private final Map<String, String> exits;

  @JsonCreator
  public RoomDescriptionDTO(
          @JsonProperty("name") String name,
          @JsonProperty("description") String description,
          @JsonProperty("objectNames") List<String> objectNames,
          @JsonProperty("occupantNames") List<String> occupantNames,
          @JsonProperty("exits") Map<String, String> exits) {
    this.name = name;
    this.description = description;
    this.objectNames = objectNames != null ? new ArrayList<>(objectNames) : new ArrayList<>();
    this.occupantNames = occupantNames != null ? new ArrayList<>(occupantNames) : new ArrayList<>();
    this.exits = exits != null ? new HashMap<>(exits) : new HashMap<>();
  }

  public String getName() {
    return name;
  }

  public String getDescription() {
    return description;
  }

  public List<String> getObjectNames() {
    return new ArrayList<>(objectNames);
  }

  public List<String> getOccupantNames() {
    return new ArrayList<>(occupantNames);
  }

  public Map<String, String> getExits() {
    return new HashMap<>(exits);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("Room: ").append(name).append("\n");
    sb.append(description).append("\n");
    sb.append("Objects: ").append(objectNames.isEmpty() ? "None" : String.join(", ", objectNames)).append("\n");
    sb.append("Occupants: ").append(occupantNames.isEmpty() ? "None" : String.join(", ", occupantNames)).append("\n");
    sb.append("Exits: ");
    if (exits.isEmpty()) {
      sb.append("None");
    } else {
      exits.forEach((dir, room) -> sb.append(dir).append(" (").append(room).append("), "));
      sb.setLength(sb.length() - 2);
    }
    return sb.toString();
  }
}