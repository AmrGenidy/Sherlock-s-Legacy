package common.commands;

import common.interfaces.GameActionContext;
import java.io.Serial;

public class ListPublicGamesCommand extends BaseCommand {
  @Serial
  private static final long serialVersionUID = 1L;

  public ListPublicGamesCommand() {
    super(false);
  }

  @Override
  protected void executeCommandLogic(GameActionContext context) {
    System.out.println("Server received ListPublicGamesCommand from player: " + getPlayerId());
  }

  @Override
  public String getDescription() {
    return "Requests a list of currently available public games to join.";
  }
}