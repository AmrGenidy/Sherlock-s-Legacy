package ui.util;

import common.dto.RoomDescriptionDTO;
import ui.MainController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class to parse game output and update the GUI accordingly.
 * This class monitors terminal output for specific patterns and triggers GUI updates.
 */
public class GameOutputParser {

  private MainController mainController;
  
  // Patterns for parsing terminal output
  private static final Pattern ROOM_PATTERN = Pattern.compile("--- Location: (.+?) ---");
  private static final Pattern CHAT_PATTERN = Pattern.compile("\\[CHAT\\] (.+?): (.+)");
  private static final Pattern JOURNAL_PATTERN = Pattern.compile("Journal Contents:");
  private static final Pattern JOURNAL_UPDATE_PATTERN = Pattern.compile("\\[JOURNAL UPDATE\\]");
  private static final Pattern ROOM_UPDATE_PATTERN = Pattern.compile("\\[ROOM_UPDATE\\]");
  private static final Pattern RESPONSE_PATTERN = Pattern.compile("(.+) says: (.+)");
  private static final Pattern RETURN_TO_CASE_SELECTION_PATTERN = Pattern.compile("Returning to case selection\\.");
  private static final Pattern RETURN_TO_MAIN_MENU_PATTERN = Pattern.compile("\\[SERVER\\] Returning to main menu\\.");

  private static final Pattern EXAM_STARTED_PATTERN = Pattern.compile("\\[EXAM_STARTED\\]");
  private static final Pattern EXAM_QUESTION_PATTERN = Pattern.compile("\\[EXAM_QUESTION\\]");
  private static final Pattern EXAM_RESULT_PATTERN = Pattern.compile("\\[EXAM_RESULT\\]");

  public GameOutputParser(MainController controller) {
    this.mainController = controller;
  }

  /**
   * Parses a line of output from the game and updates GUI components.
   */
  public void parseLine(String line) {
    if (line == null || line.trim().isEmpty()) {
      return;
    }

    // Check for room changes
    Matcher roomMatcher = ROOM_PATTERN.matcher(line);
    if (roomMatcher.find()) {
      String roomName = roomMatcher.group(1);
      // Note: We would need the full RoomDescriptionDTO here
      // In a real implementation, this would be passed from GameClient when it receives the DTO
      return;
    }

    // Check for chat messages
    Matcher chatMatcher = CHAT_PATTERN.matcher(line);
    if (chatMatcher.find()) {
      String sender = chatMatcher.group(1);
      String message = chatMatcher.group(2);
      mainController.addChatMessage(sender, message);
      return;
    }

    // Check for journal updates
    Matcher journalMatcher = JOURNAL_PATTERN.matcher(line);
    if (journalMatcher.find()) {
      mainController.refreshJournalWindow();
      return;
    }

    Matcher roomUpdateMatcher = ROOM_UPDATE_PATTERN.matcher(line);
    if (roomUpdateMatcher.find()) {
      mainController.refreshRoomView();
      return;
    }

    Matcher journalUpdateMatcher = JOURNAL_UPDATE_PATTERN.matcher(line);
    if (journalUpdateMatcher.find()) {
      mainController.refreshJournalWindow();
      return;
    }

    // Check for NPC responses
    Matcher responseMatcher = RESPONSE_PATTERN.matcher(line);
    if (responseMatcher.find()) {
      String npcName = responseMatcher.group(1);
      String response = responseMatcher.group(2);
      mainController.showRoomResponse(npcName, response);
      return;
    }

    // Check for return to menu
    Matcher returnCaseMatcher = RETURN_TO_CASE_SELECTION_PATTERN.matcher(line);
    if (returnCaseMatcher.find()) {
      mainController.showCaseSelectionMenu();
      return;
    }

    Matcher returnMainMenuMatcher = RETURN_TO_MAIN_MENU_PATTERN.matcher(line);
    if (returnMainMenuMatcher.find()) {
      mainController.returnToMultiplayerMenu();
      return;
    }

    Matcher examStartedMatcher = EXAM_STARTED_PATTERN.matcher(line);
    if (examStartedMatcher.find()) {
        mainController.onSinglePlayerExamStarted();
        return;
    }

    Matcher examQuestionMatcher = EXAM_QUESTION_PATTERN.matcher(line);
    if (examQuestionMatcher.find()) {
        mainController.onSinglePlayerQuestionUpdate();
        return;
    }

    Matcher examResultMatcher = EXAM_RESULT_PATTERN.matcher(line);
    if (examResultMatcher.find()) {
        mainController.onSinglePlayerExamResult();
        return;
    }
  }

  /**
   * Parses room description from terminal output and creates a DTO.
   * This is a fallback for when RoomDescriptionDTO is not directly available.
   */
  public static RoomDescriptionDTO parseRoomFromText(String roomText) {
    // Parse room name
    Pattern namePattern = Pattern.compile("Room: (.+)");
    Matcher nameMatcher = namePattern.matcher(roomText);
    String roomName = nameMatcher.find() ? nameMatcher.group(1) : "Unknown Room";

    // Parse description
    Pattern descPattern = Pattern.compile("Room: .+?\n(.+?)\n");
    Matcher descMatcher = descPattern.matcher(roomText);
    String description = descMatcher.find() ? descMatcher.group(1) : "";

    // Parse objects
    Pattern objPattern = Pattern.compile("Objects: (.+)");
    Matcher objMatcher = objPattern.matcher(roomText);
    List<String> objects = new ArrayList<>();
    if (objMatcher.find()) {
      String objText = objMatcher.group(1);
      if (!objText.equals("None")) {
        for (String obj : objText.split(", ")) {
          objects.add(obj.trim());
        }
      }
    }

    // Parse occupants
    Pattern occPattern = Pattern.compile("Occupants: (.+)");
    Matcher occMatcher = occPattern.matcher(roomText);
    List<String> occupants = new ArrayList<>();
    if (occMatcher.find()) {
      String occText = occMatcher.group(1);
      if (!occText.equals("None")) {
        for (String occ : occText.split(", ")) {
          occupants.add(occ.trim());
        }
      }
    }

    // Parse exits
    Pattern exitPattern = Pattern.compile("Exits: (.+)");
    Matcher exitMatcher = exitPattern.matcher(roomText);
    Map<String, String> exits = new HashMap<>();
    if (exitMatcher.find()) {
      String exitText = exitMatcher.group(1);
      if (!exitText.equals("None")) {
        // Parse exits like "north (to Library), south (to Hallway)"
        Pattern singleExitPattern = Pattern.compile("(\\w+) \\(to (.+?)\\)");
        Matcher singleExitMatcher = singleExitPattern.matcher(exitText);
        while (singleExitMatcher.find()) {
          String direction = singleExitMatcher.group(1);
          String destination = singleExitMatcher.group(2);
          exits.put(direction, destination);
        }
      }
    }

    return new RoomDescriptionDTO(roomName, description, objects, occupants, exits);
  }
}
