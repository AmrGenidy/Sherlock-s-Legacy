package common.commands;

import common.interfaces.GameActionContext;
import java.io.Serial;

public class ContinueGameCommand extends BaseCommand {
    @Serial
    private static final long serialVersionUID = 1L;

    public ContinueGameCommand() {
        super(false);
    }

    @Override
    protected void executeCommandLogic(GameActionContext context) {
        context.processContinueGame(getPlayerId());
    }

    @Override
    public String getDescription() {
        return "Continues the game after a major event (e.g., Final Exam), refreshing the view for all players.";
    }
}
