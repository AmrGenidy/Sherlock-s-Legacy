package singleplayer.util;

// This parser focuses on identifying the command name and basic tokenization.
// Argument validation (e.g., ensuring 'move' has a direction) is often
// handled by the command factory or the command constructor itself.
public class CommandParserSinglePlayer {

  // Simpler version, if you want to handle multi-word args more generally in factory
  public static String[] parseInputSimple(String input) {
    if (input == null || input.trim().isEmpty()) {
      return new String[0];
    }
    // Normalize: lowercase, trim, collapse multiple spaces
    String normalizedInput = input.trim().replaceAll("\\s+", " ").toLowerCase();

    // Handle specific multi-word commands first
    if (normalizedInput.startsWith("journal add ")) {
      return new String[] {
        "journal add", normalizedInput.substring("journal add ".length()).trim()
      };
    }
    switch (normalizedInput) {
      case "start case" -> {
        return new String[] {"start case"};
      }
      case "ask watson" -> {
        return new String[] {"ask watson"};
      }
      case "final exam" -> {
        return new String[] {"final exam"}; // Will map to InitiateFinalExamCommand
      }
    }

    // For single-word commands or commands where the first word is the command
    // and the rest is a single argument string.
    return normalizedInput.split(" ", 2);
  }
}
