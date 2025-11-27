package common.commands;

import common.dto.TextMessage;
import common.interfaces.GameActionContext;
import java.io.Serial;
import java.util.LinkedHashMap;
import java.util.Map;

public class HelpCommand extends BaseCommand {
  @Serial
  private static final long serialVersionUID = 1L;

  public HelpCommand() {
    super(false);
  }

  @Override
  protected void executeCommandLogic(GameActionContext context) {
    Map<String, String> commandsToShow = new LinkedHashMap<>();
    // NOTE: This command is now only usable in-game. The server-side GameSession
    // class now blocks this command from being used in a pre-game lobby.
    commandsToShow.put("look", "View surroundings.");
    commandsToShow.put("move [direction]", "Move to another room.");
    commandsToShow.put("examine [object]", "Inspect an object.");
    commandsToShow.put("question [suspect]", "Question a suspect.");
    commandsToShow.put("deduce [object]", "Make a deduction about an object.");
    commandsToShow.put("journal", "View your journal.");
    commandsToShow.put("journal add [note]", "Add a note to your journal.");
    commandsToShow.put("tasks", "View case tasks.");
    commandsToShow.put("ask watson", "Ask Dr. Watson for a hint.");
    commandsToShow.put("final exam", "Initiate the final exam (if conditions met).");
    commandsToShow.put("exit", "Exit the current case (MP) or game (SP).");
    commandsToShow.put("help", "Display this help message.");

    StringBuilder helpMessage = new StringBuilder("Available commands:\n");
    for (Map.Entry<String, String> entry : commandsToShow.entrySet()) {
      helpMessage.append(String.format("  %-28s - %s\n", entry.getKey(), entry.getValue()));
    }
    context.sendResponseToPlayer(getPlayerId(), new TextMessage(helpMessage.toString().trim(), false));
  }

  @Override
  public String getDescription() {
    return "Displays a list of available commands and their descriptions.";
  }
}