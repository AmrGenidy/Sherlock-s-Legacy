package common.commands;

import Core.GameObject;
import Core.Room;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import common.dto.JournalEntryDTO;
import common.dto.TextMessage;
import common.interfaces.GameActionContext;
import java.io.Serial;

public class ExamineCommand extends BaseCommand {
  @Serial
  private static final long serialVersionUID = 1L;
  private final String objectName;

  @JsonCreator
  public ExamineCommand(@JsonProperty("objectName") String objectName) {
    super(true);
    if (objectName == null || objectName.trim().isEmpty()) {
      throw new IllegalArgumentException("Object name cannot be null or empty for ExamineCommand.");
    }
    this.objectName = objectName.trim();
  }

  public String getObjectName() {
    return objectName;
  }

  @Override
  protected void executeCommandLogic(GameActionContext context) {
    Room currentRoom = context.getCurrentRoomForPlayer(getPlayerId());
    if (currentRoom == null) {
      context.sendResponseToPlayer(getPlayerId(), new TextMessage("Error: You are not in a valid room.", true));
      return;
    }

    GameObject objectToExamine = currentRoom.getObject(this.objectName);
    if (objectToExamine == null) {
      context.sendResponseToPlayer(getPlayerId(), new TextMessage("There is no '" + this.objectName + "' to examine here.", false));
      return;
    }

    String examinationResult = objectToExamine.getExamine();
    if (examinationResult == null || examinationResult.trim().isEmpty()) {
      examinationResult = objectToExamine.getDescription();
    }

    String messageText = "You examine the " + objectToExamine.getName() + ": " + examinationResult;
    context.sendResponseToPlayer(getPlayerId(), new TextMessage(messageText, false));

    String journalText = "Examined " + objectToExamine.getName() + ": " + examinationResult;
    context.addJournalEntry(new JournalEntryDTO(journalText, getPlayerId(), System.currentTimeMillis()));
  }

  @Override
  public String getDescription() {
    return "Inspects an object for clues. Usage: examine [object_name]";
  }
}