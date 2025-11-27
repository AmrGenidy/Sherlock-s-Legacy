package extractors; // Or your chosen package for extractors

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import JsonDTO.CaseData;

import Core.Room;
import Core.Suspect;
import JsonDTO.CaseFile;
import common.interfaces.GameContext;

// ... other necessary imports for loadSuspects if it's in this file ...

public class SuspectExtractor {

  // Custom Exception for No Valid Rooms (if not already defined elsewhere)
  public static class NoValidRoomsException extends Exception {
    public NoValidRoomsException(String message) {
      super(message);
    }
  }

  private SuspectExtractor() {} // Utility class

  // Assuming your loadSuspects method is also in this class
  public static void loadSuspects(CaseData caseFile, GameContext context) throws NoValidRoomsException { // Make it throw NoValidRoomsException
    if (caseFile == null || caseFile.getSuspects() == null) {
      context.logLoadingMessage(
              "Warning: No suspects defined in case file or case file is null for "
                      + context.getContextIdForLog());
      return;
    }

    Set<String> suspectNames = new HashSet<>(); // To check for duplicates

    for (CaseFile.SuspectData suspectData : caseFile.getSuspects()) {
      String suspectName = suspectData.getName();
      if (suspectName == null || suspectName.trim().isEmpty()) {
        context.logLoadingMessage(
                "Warning: Skipping suspect with null or empty name in " + context.getContextIdForLog());
        continue;
      }
      if (!suspectNames.add(suspectName.toLowerCase())) {
        context.logLoadingMessage(
                "Warning: Duplicate suspect name '"
                        + suspectName
                        + "' found. Skipping duplicate in "
                        + context.getContextIdForLog());
        continue;
      }

      Suspect suspect =
              new Suspect(suspectData.getName(), suspectData.getStatement(), suspectData.getClue());

      try {
        Room startingRoom =
                assignRandomStartingRoom(suspect, context, context.getContextIdForLog());
        suspect.setCurrentRoom(startingRoom);
        context.addSuspect(suspect);
      } catch (NoValidRoomsException e) {
        // Log it, and decide if this is critical.
        // If one suspect can't be placed, it might be okay to continue, or you might want to fail
        // loading.
        // For now, let's log and re-throw if loadSuspects is declared to throw it.
        context.logLoadingMessage(
                "Error placing suspect '"
                        + suspect.getName()
                        + "': "
                        + e.getMessage()
                        + " for "
                        + context.getContextIdForLog());
        throw e; // Or handle by skipping this suspect and continuing
      }
    }
  }

  /**
   * Assigns a random starting room to a suspect from the available rooms in the context.
   *
   * @param suspect The suspect to assign a room to.
   * @param context The game context containing all available rooms.
   * @param contextId A string identifier for the context, for logging purposes.
   * @return A randomly selected Room.
   * @throws NoValidRoomsException if no rooms are available in the context.
   */
  private static Room assignRandomStartingRoom(
          Suspect suspect, GameContext context, String contextId) throws NoValidRoomsException {

    if (context == null || context.getAllRooms() == null) {
      throw new NoValidRoomsException(
              "Game context or room list is null, cannot assign room for suspect: "
                      + suspect.getName()
                      + " in "
                      + contextId);
    }

    // CORRECTED LINE:
    Collection<Room> allRoomsCollection = context.getAllRooms().values();

    if (allRoomsCollection.isEmpty()) {
      throw new NoValidRoomsException(
              "No rooms available in context '"
                      + contextId
                      + "' to assign to suspect: "
                      + suspect.getName());
    }

    // Convert collection to list to get random element by index
    List<Room> roomList = new ArrayList<>(allRoomsCollection);

    // Optional: Filter out rooms if SuspectData had 'allowedRooms' or 'disallowedRooms'
    // For now, just pick from any available room.

    return roomList.get(new Random().nextInt(roomList.size()));
  }
}
