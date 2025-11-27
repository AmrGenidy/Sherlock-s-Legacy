package common.commands;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import common.dto.JournalEntryDTO;
import common.dto.TextMessage;
import common.interfaces.GameActionContext;
import java.io.Serial;

public class JournalAddCommand extends BaseCommand {
  @Serial
  private static final long serialVersionUID = 1L;
  private final String note;

  @JsonCreator
  public JournalAddCommand(@JsonProperty("note") String note) {
    super(true);
    if (note == null || note.trim().isEmpty()) {
      throw new IllegalArgumentException("Note cannot be null or empty for JournalAddCommand.");
    }
    this.note = note.trim();
  }


  public String getNote() {
    return note;
  }

  @Override
  protected void executeCommandLogic(GameActionContext context) {
    JournalEntryDTO newEntry = new JournalEntryDTO(this.note, getPlayerId(), System.currentTimeMillis());
    context.addJournalEntry(newEntry);


    // Send a simple confirmation message to the terminal
    TextMessage confirmation = new TextMessage("Note added to journal.", false);
    context.sendResponseToPlayer(getPlayerId(), confirmation);
  }

  @Override
  public String getDescription() {
    return "Adds a custom note to your journal. Usage: journal add [your note text]";
  }
}