package common.commands;

import Core.GameObject;
import Core.Room;
import common.dto.RoomDescriptionDTO;
import common.dto.TextMessage;
import common.interfaces.GameActionContext;
import java.io.Serial;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class LookCommand extends BaseCommand {
  @Serial
  private static final long serialVersionUID = 1L;

  public LookCommand() {
    super(true);
  }

  @Override
  protected void executeCommandLogic(GameActionContext context) {
    Room currentRoom = context.getCurrentRoomForPlayer(getPlayerId());
    if (currentRoom == null) {
      context.sendResponseToPlayer(getPlayerId(), new TextMessage("Error: You are not in a valid room.", true));
      return;
    }

    List<String> objectNamesInRoom = currentRoom.getObjects().values().stream()
            .map(GameObject::getName)
            .collect(Collectors.toList());

    String occupantsString = context.getOccupantsDescriptionInRoom(currentRoom, getPlayerId());
    List<String> occupantNames = new ArrayList<>();
    if (occupantsString != null && !occupantsString.equalsIgnoreCase("Occupants: None") && occupantsString.startsWith("Occupants: ")) {
      String[] names = occupantsString.substring("Occupants: ".length()).split(",\\s*");
      for (String name : names) {
        if (!name.trim().isEmpty()) {
          occupantNames.add(name.trim());
        }
      }
    }

    Map<String, String> exitsMap = currentRoom.getNeighbors().entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().getName()));

    RoomDescriptionDTO roomDTO = new RoomDescriptionDTO(
            currentRoom.getName(),
            currentRoom.getDescription(),
            objectNamesInRoom,
            occupantNames,
            exitsMap);
    context.sendResponseToPlayer(getPlayerId(), roomDTO);
  }

  @Override
  public String getDescription() {
    return "Views your surroundings (current room's description, objects, exits, and occupants).";
  }
}