package singleplayer.util;

import common.commands.*;

// No multiplayer DTOs needed for a single-player factory.

/**
 * CommandFactorySinglePlayer Creates Command objects for the Single Player mode based on parsed
 * user input.
 */
public class CommandFactorySinglePlayer {

  // Utility class - no instances.
  private CommandFactorySinglePlayer() {}

  /**
   * Creates a Command based on parsed input.
   *
   * @param parsedInput String array: [0] is command name, [1] (optional) is argument.
   * @return A Command object, or null if input is invalid or command unknown.
   */
  public static Command createCommand(String[] parsedInput) {
    if (parsedInput == null || parsedInput.length == 0) {
      return null;
    }

    String commandName = parsedInput[0].toLowerCase();
    String arg = (parsedInput.length > 1) ? parsedInput[1] : null;

    return switch (commandName) {
      case "look" -> new LookCommand();
      case "move" -> {
        if (arg != null && !arg.isEmpty()) yield new MoveCommand(arg);
        else System.out.println("Usage: move <direction>");
        yield null;
      }
      case "examine" -> {
        if (arg != null && !arg.isEmpty()) yield new ExamineCommand(arg);
        else System.out.println("Usage: examine <object_name>");
        yield null;
      }
      case "question" -> {
        if (arg != null && !arg.isEmpty()) yield new QuestionCommand(arg);
        else System.out.println("Usage: question <suspect_name>");
        yield null;
      }
      case "journal" -> new JournalCommand(arg); // Null arg means view all.
      case "journal add" -> {
        if (arg != null && !arg.isEmpty()) yield new JournalAddCommand(arg);
        else System.out.println("Usage: journal add <note_text>");
        yield null;
      }
      case "deduce" -> {
        if (arg != null && !arg.isEmpty()) yield new DeduceCommand(arg);
        else System.out.println("Usage: deduce <object_name>");
        yield null;
      }
      case "ask watson" -> new AskWatsonCommand();
      case "final exam" -> // User types "final exam"
          new InitiateFinalExamCommand(); // SP context will handle this.
      case "submit answer" -> {
        // This is handled directly by the SP game loop when answering questions.
        System.out.println("Error: 'submit answer' is used automatically during the exam Q&A.");
        yield null;
      }
      case "tasks" -> new TaskCommand();
      case "help" -> new HelpCommand();
      case "start case" -> new StartCaseCommand();
      case "exit" -> new ExitCommand();
      case "add case" -> {
        // 'add case' requires specific file system interaction.
        // Handled by SinglePlayerMain using CaseFileUtil or a dedicated SP command.
        // This factory won't create a common.commands.AddCaseCommand for SP
        // unless that command is designed to work with a local SP context.
        if (arg != null && !arg.isEmpty()) {
          System.out.println(
              "Note: 'add case' uses utility. For manual typing, ensure path is correct.");
          // Example: return new AddCaseSPCommand(arg); // If you had a specific SP version
          // Or, SinglePlayerMain calls CaseFileUtil.addCaseFromFile(arg) directly.
          // For now, returning null as SPMain handles "add case" input directly.
        } else {
          System.out.println("Usage: add case <file_path>");
        }
        yield null;
      }
      default ->
          // Unknown command. SinglePlayerMain's loop will inform the user.
          null;
    };
  }
}
