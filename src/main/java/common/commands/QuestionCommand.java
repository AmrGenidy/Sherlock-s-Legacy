package common.commands;

import Core.Room;
import Core.Suspect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import common.dto.JournalEntryDTO;
import common.dto.TextMessage;
import common.interfaces.GameActionContext;
import java.io.Serial;
import java.util.List;
import java.util.stream.Collectors;

public class QuestionCommand extends BaseCommand {
  @Serial
  private static final long serialVersionUID = 1L;
  private final String suspectName;

  @JsonCreator
  public QuestionCommand(@JsonProperty("suspectName") String suspectName) {
    super(true);
    if (suspectName == null || suspectName.trim().isEmpty()) {
      throw new IllegalArgumentException("Suspect name cannot be null or empty for QuestionCommand.");
    }
    this.suspectName = suspectName.trim();
  }

  public String getSuspectName() {
    return suspectName;
  }

  @Override
  protected void executeCommandLogic(GameActionContext context) {
    Room currentRoom = context.getCurrentRoomForPlayer(getPlayerId());
    if (currentRoom == null) {
      context.sendResponseToPlayer(getPlayerId(), new TextMessage("Error: You are not in a valid room to question anyone.", true));
      return;
    }

    List<Suspect> suspectsInRoom = context.getAllSuspects().stream()
            .filter(s -> s.getCurrentRoom() != null && s.getCurrentRoom().getName().equalsIgnoreCase(currentRoom.getName()))
            .filter(s -> s.getName().equalsIgnoreCase(this.suspectName))
            .collect(Collectors.toList());

    if (suspectsInRoom.isEmpty()) {
      context.sendResponseToPlayer(getPlayerId(), new TextMessage("Suspect '" + this.suspectName + "' is not in this room.", false));
      return;
    }

    Suspect targetSuspect = suspectsInRoom.get(0);
    String statement = targetSuspect.getStatement();
    if (statement == null || statement.trim().isEmpty()) {
      statement = targetSuspect.getName() + " has nothing to say or seems unwilling to talk right now.";
    }

    String messageText = targetSuspect.getName() + " says: \"" + statement + "\"";
    context.sendResponseToPlayer(getPlayerId(), new TextMessage(messageText, false));

    String journalText = "Questioned " + targetSuspect.getName() + ": " + statement;
    context.addJournalEntry(new JournalEntryDTO(journalText, getPlayerId(), System.currentTimeMillis()));
    context.sendResponseToPlayer(getPlayerId(), new TextMessage("(You can now try to 'deduce " + targetSuspect.getName() + "' based on their statement.)", false));
  }

  @Override
  public String getDescription() {
    return "Questions a suspect in the current room. Usage: question [suspect_name]";
  }
}