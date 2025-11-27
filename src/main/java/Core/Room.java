package Core;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class Room implements Serializable {
  // Name is final, set once. Good. Description can change if needed.
  private final String name;
  private String description;
  // Using protected so subclasses (if I ever make them for special rooms) can directly access,
  // but generally, I'll use the public methods.
  protected Map<String, Room> neighbors = new HashMap<>(); // Direction (lowercase) -> Neighbor Room
  protected Map<String, GameObject> objects =
      new HashMap<>(); // Object Name (lowercase) -> GameObject

  /**
   * My main constructor for a Room.
   *
   * @param name The unique name of this room. Can't be null or empty, obviously.
   * @param description The text description players see when they enter or look.
   */
  public Room(String name, String description) {
    if (name == null || name.trim().isEmpty()) {
      // Don't want rooms without names, that'd be a mess.
      throw new IllegalArgumentException("Room name cannot be null or empty.");
    }
    this.name = name.trim(); // Trim it just in case.
    this.description = description;
  }

  public String getName() {
    return name;
  }

  public String getDescription() {
    return description;
  }

  /**
   * Allows changing the room's description dynamically if the game needs it.
   *
   * @param description The new description text.
   */
  public void setDescription(String description) {
    this.description = description;
  }

  /**
   * Gets a map of neighboring rooms.
   *
   * @return A new Map instance to prevent external modification of my internal neighbors map.
   */
  public Map<String, Room> getNeighbors() {
    // Always return a copy. Don't want anyone messing with my internal map directly.
    return new HashMap<>(neighbors);
  }

  /**
   * Sets a neighbor in a specific direction. Direction is stored as lowercase.
   *
   * @param direction The direction (e.g., "north", "south").
   * @param neighbor The Room object that is the neighbor.
   */
  public void setNeighbor(String direction, Room neighbor) {
    if (direction == null || direction.trim().isEmpty() || neighbor == null) {
      // Basic check, probably log this error instead of just System.err if it was a bigger app.
      System.err.println(
          "ROOM_ERROR: Invalid direction or null neighbor for setNeighbor in room '"
              + this.name
              + "'.");
      return;
    }
    neighbors.put(direction.trim().toLowerCase(), neighbor);
  }

  /**
   * Gets the neighboring room in a given direction.
   *
   * @param direction The direction to check (case-insensitive).
   * @return The neighboring Room, or null if no exit in that direction.
   */
  public Room getNeighbor(String direction) {
    if (direction == null) return null;
    return neighbors.get(direction.trim().toLowerCase());
  }

  /**
   * Retrieves a specific GameObject from this room by its name.
   *
   * @param objectName The name of the object (case-insensitive).
   * @return The GameObject, or null if not found.
   */
  public GameObject getObject(String objectName) {
    if (objectName == null) return null;
    return objects.get(objectName.trim().toLowerCase());
  }

  /**
   * Adds a GameObject to this room. The object's name (converted to lowercase) is used as the key.
   *
   * @param object The GameObject to add.
   */
  public void addObject(GameObject object) {
    if (object == null || object.getName() == null || object.getName().trim().isEmpty()) {
      System.err.println(
          "ROOM_ERROR: Invalid object (null or no name) for addObject in room '"
              + this.name
              + "'.");
      return;
    }
    // Store object by its name, lowercase, for easy lookup.
    objects.put(object.getName().trim().toLowerCase(), object);
  }

  /**
   * Gets all GameObjects present in this room.
   *
   * @return A new Map instance to prevent external modification of my internal objects map.
   */
  public Map<String, GameObject> getObjects() {
    // Defensive copy again. Good habit.
    return new HashMap<>(objects);
  }

  // Standard equals and hashCode based on the room's 'name'.
  // This is important if I store Room objects in Sets or use them as keys in Maps
  // where uniqueness is determined by name.
  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Room room = (Room) o;
    return name.equals(room.name); // Names are unique identifiers for rooms.
  }

  @Override
  public int hashCode() {
    return name.hashCode(); // Consistent with equals.
  }

  /**
   * Simple toString for debugging or logging Room objects.
   *
   * @return String representation of the room.
   */
  @Override
  public String toString() {
    // Just name and description, don't need full neighbors/objects here for a quick look.
    return "Room{name='"
        + name
        + "', description_preview='"
        + (description != null
            ? description.substring(0, Math.min(description.length(), 30)) + "..."
            : "N/A")
        + "'}";
  }
}
