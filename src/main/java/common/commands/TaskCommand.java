package common.commands;

import Core.TaskList;
import common.dto.TextMessage;
import common.interfaces.GameActionContext;
import java.io.Serial;
import java.util.List;

public class TaskCommand extends BaseCommand {
  @Serial
  private static final long serialVersionUID = 1L;

  public TaskCommand() {
    super(true);
  }

  @Override
  protected void executeCommandLogic(GameActionContext context) {
    TaskList taskList = context.getTaskList();
    if (taskList == null || taskList.getTasks().isEmpty()) {
      context.sendResponseToPlayer(getPlayerId(), new TextMessage("No tasks available for this case.", false));
      return;
    }

    List<String> tasks = taskList.getTasks();
    StringBuilder taskMessage = new StringBuilder("--- Case Tasks ---\n");
    for (int i = 0; i < tasks.size(); i++) {
      taskMessage.append((i + 1)).append(". ").append(tasks.get(i)).append("\n");
    }
    context.sendResponseToPlayer(getPlayerId(), new TextMessage(taskMessage.toString().trim(), false));
  }

  @Override
  public String getDescription() {
    return "Displays the list of tasks for the current case.";
  }
}