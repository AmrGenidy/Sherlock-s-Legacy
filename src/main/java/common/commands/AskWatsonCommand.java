package common.commands;

import common.dto.TextMessage;
import common.dto.WatsonHintResponseDTO;
import common.interfaces.GameActionContext;
import java.io.Serial;

public class AskWatsonCommand extends BaseCommand {
  @Serial
  private static final long serialVersionUID = 1L;

  public AskWatsonCommand() {
    super(true); // Requires case to be started
  }

  @Override
  protected void executeCommandLogic(GameActionContext context) {
    WatsonHintResponseDTO watsonResponse = context.askWatsonForHint(getPlayerId());
    if (watsonResponse == null) {
      context.sendResponseToPlayer(getPlayerId(), new TextMessage("Error receiving response from Watson.", true));
      return;
    }

    String messageContent = watsonResponse.getMessage();
    if (watsonResponse.isActualHint()) {
      String formattedHintMessage = "Watson: \"" + messageContent + "\"";
      context.sendResponseToPlayer(getPlayerId(), new TextMessage(formattedHintMessage, false));
    } else {
      context.sendResponseToPlayer(getPlayerId(), new TextMessage(messageContent, false));
    }
  }

  @Override
  public String getDescription() {
    return "Asks Dr. Watson for a hint if he is in the same room.";
  }
}
