package common.commands;

import Core.Detective;
import Core.GameObject;
import Core.Room;
import Core.Suspect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import common.dto.JournalEntryDTO;
import common.dto.TextMessage;
import common.interfaces.GameActionContext;
import java.io.Serial;
import java.util.List;
import java.util.Optional;

public class DeduceCommand extends BaseCommand {
    @Serial
    private static final long serialVersionUID = 1L;
    private final String targetName; // Renamed from objectName

    @JsonCreator
    public DeduceCommand(@JsonProperty("targetName") String targetName) {
        super(true);
        if (targetName == null || targetName.trim().isEmpty()) {
            throw new IllegalArgumentException("Target name cannot be null or empty for DeduceCommand.");
        }
        this.targetName = targetName.trim();
    }

    public String getTargetName() {
        return targetName;
    }

    @Override
    protected void executeCommandLogic(GameActionContext context) {
        Detective playerDetective = context.getPlayerDetective(getPlayerId());
        Room currentRoom = context.getCurrentRoomForPlayer(getPlayerId());

        if (currentRoom == null || playerDetective == null) {
            context.sendResponseToPlayer(getPlayerId(), new TextMessage("Error: Cannot perform deduction. Invalid player or room state.", true));
            return;
        }

        // --- NEW UNIFIED LOGIC ---

        // Step 1: Check if the target is a GameObject in the room.
        GameObject objectToDeduce = currentRoom.getObject(this.targetName);
        if (objectToDeduce != null) {
            handleDeduceGameObject(context, playerDetective, objectToDeduce);
            return;
        }

        // Step 2: If not an object, check if it's a Suspect in the room.
        // We get ALL suspects and filter for those in the current room.
        Optional<Suspect> suspectToDeduce = context.getAllSuspects().stream()
                .filter(s -> s.getCurrentRoom() != null && s.getCurrentRoom().equals(currentRoom))
                .filter(s -> s.getName().equalsIgnoreCase(this.targetName))
                .findFirst();

        if (suspectToDeduce.isPresent()) {
            handleDeduceSuspect(context, playerDetective, suspectToDeduce.get());
            return;
        }

        // Step 3: If it's neither, the target is not here.
        context.sendResponseToPlayer(getPlayerId(), new TextMessage("There is no '" + this.targetName + "' here to deduce from.", false));
    }

    // --- HELPER METHODS FOR CLEANLINESS ---
    private void handleDeduceGameObject(GameActionContext context, Detective detective, GameObject object) {
      // This method still tracks *per-player* who deduced what, to prevent a player from
      // getting points/clues for something their partner already deduced.
      if (!detective.incrementDeduceCount(object.getName())) {
          context.sendResponseToPlayer(getPlayerId(), new TextMessage("You have already made a deduction about the " + object.getName() + ".", false));
          return;
      }
  
      // Increment the shared, session-wide counter. THIS IS THE KEY CHANGE.
      context.incrementSessionDeduceCount();
  
      String clue = object.getDeduce();
      if (clue == null || clue.trim().isEmpty()) {
          clue = "You ponder about the " + object.getName() + " but gain no new insights.";
      }
      
      String messageText = "Deduction from " + object.getName() + ": " + clue;
      context.sendResponseToPlayer(getPlayerId(), new TextMessage(messageText, false));
  
      String journalText = "Deduced from " + object.getName() + ": " + clue;
      context.addJournalEntry(new JournalEntryDTO(journalText, getPlayerId(), System.currentTimeMillis()));
  
      // Display the NEW, SHARED team counter.
      context.sendResponseToPlayer(getPlayerId(), new TextMessage("Team deductions used: " + context.getSessionDeduceCount(), false));
  }

  private void handleDeduceSuspect(GameActionContext context, Detective detective, Suspect suspect) {
    // This method still tracks *per-player* who deduced what.
    if (!detective.incrementDeduceCount(suspect.getName())) {
        context.sendResponseToPlayer(getPlayerId(), new TextMessage("You have already made a deduction about " + suspect.getName() + ".", false));
        return;
    }

    // Increment the shared, session-wide counter. THIS IS THE KEY CHANGE.
    context.incrementSessionDeduceCount();

    String clue = suspect.getClue();
    if (clue == null || clue.trim().isEmpty()) {
        clue = "You observe " + suspect.getName() + " carefully, but gain no new insights beyond their statement.";
    }

    String messageText = "Deduction about " + suspect.getName() + ": " + clue;
    context.sendResponseToPlayer(getPlayerId(), new TextMessage(messageText, false));

    String journalText = "Deduced about " + suspect.getName() + ": " + clue;
    context.addJournalEntry(new JournalEntryDTO(journalText, getPlayerId(), System.currentTimeMillis()));

    // Display the NEW, SHARED team counter.
    context.sendResponseToPlayer(getPlayerId(), new TextMessage("Team deductions used: " + context.getSessionDeduceCount(), false));
}


    @Override
    public String getDescription() {
        return "Makes a deduction about an object or suspect, revealing a clue. Affects rank. Usage: deduce [target_name]";
    }

}