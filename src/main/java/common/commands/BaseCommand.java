package common.commands;

import com.fasterxml.jackson.annotation.JsonProperty;
import common.dto.TextMessage;
import common.interfaces.GameActionContext;

public abstract class BaseCommand implements Command {
  protected String playerId;
  protected final boolean requiresCaseStarted;

  public BaseCommand(@JsonProperty("requiresCaseStarted") boolean requiresCaseStarted) {
    this.requiresCaseStarted = requiresCaseStarted;
  }

  @Override
  public String getPlayerId() {
    return playerId;
  }

  @Override
  public void setPlayerId(String playerId) {
    this.playerId = playerId;
  }

  public boolean isRequiresCaseStarted() {
    return requiresCaseStarted;
  }

  @Override
  public final void execute(GameActionContext context) {
    if (playerId == null || playerId.trim().isEmpty()) {
      System.err.println("Error: Player ID not set for command: " + getClass().getSimpleName());
      return;
    }
    if (requiresCaseStarted && !context.isCaseStarted()) {
      context.sendResponseToPlayer(
              playerId,
              new TextMessage("The case has not started yet. Use 'start case' to begin.", true));
      return;
    }
    executeCommandLogic(context);
  }

  protected abstract void executeCommandLogic(GameActionContext context);
}
