package extractors;

import Core.GameObject;
import Core.Room;
import JsonDTO.CaseFile;
import common.interfaces.GameContext;
import java.util.Objects;
import JsonDTO.CaseData;

/**
 * GameObjectExtractor My job: take the GameObjectData from a CaseFile (parsed from JSON) and turn
 * it into actual GameObject instances, then stick them into the correct Room objects within the
 * provided GameContext. Pretty straightforward.
 */
public class GameObjectExtractor {

  // Utility class, so private constructor to prevent anyone from making an instance.
  private GameObjectExtractor() {}

  /**
   * Loads all game objects from the case file into their respective rooms in the game context. It's
   * static because it's a utility method; doesn't need instance state.
   *
   * @param caseFile The CaseFile DTO containing all data for the current case. Must not be null.
   * @param context The GameContext (either SinglePlayer or Server) where rooms are already loaded
   *     and where these new GameObjects will be added. Must not be null.
   */
  public static void loadObjects(CaseData caseFile, GameContext context) {
    // Fail fast if essential inputs are missing. No point continuing.
    Objects.requireNonNull(caseFile, "CaseFile cannot be null for GameObjectExtractor.");
    Objects.requireNonNull(context, "GameContext cannot be null for GameObjectExtractor.");

    // Log the start of the process. Good for tracking load times or issues.
    context.logLoadingMessage("Starting to load game objects...");
    int objectsLoaded = 0;
    int roomNotFoundErrors = 0;
    int invalidObjectDataErrors = 0;

    // If the CaseFile has no 'rooms' array, then there's nowhere to put objects.
    if (caseFile.getRooms() == null) {
      context.logLoadingMessage("Error: CaseFile contains no 'rooms' data. Cannot load objects.");
      return;
    }




    // Loop through each room defined in the case file.
    for (CaseFile.RoomData roomData : caseFile.getRooms()) {
      // Basic sanity check for the room data itself.
      if (roomData == null || roomData.getName() == null || roomData.getName().trim().isEmpty()) {
        context.logLoadingMessage(
                "Warning: Skipping a room with null or empty name in CaseFile during object loading.");
        // Don't count this as a roomNotFound error, just bad data in the case file.
        continue;
      }

      // Try to find this room in the context (it should have been loaded by BuildingExtractor
      // already).
      Room room = context.getRoomByName(roomData.getName());

      if (room != null) {
        // Room found! Now check if it has any objects defined.
        if (roomData.getObjects() != null) {
          for (CaseFile.GameObjectData objData : roomData.getObjects()) {
            // Sanity check for individual object data.
            if (objData == null) {
              context.logLoadingMessage(
                      "Warning: Found null GameObjectData in room '"
                              + roomData.getName()
                              + "'. Skipping.");
              invalidObjectDataErrors++;
              continue;
            }
            if (objData.getName() == null || objData.getName().trim().isEmpty()) {
              context.logLoadingMessage(
                      "Warning: GameObject in room '"
                              + roomData.getName()
                              + "' has no name. Skipping.");
              invalidObjectDataErrors++;
              continue;
            }

            // Providing default values if JSON fields are missing or empty.
            // This makes the case files more forgiving.
            String name = objData.getName().trim(); // Always trim names.
            String description =
                    (objData.getDescription() != null && !objData.getDescription().trim().isEmpty())
                            ? objData.getDescription()
                            : "A nondescript " + name + "."; // Sensible default.
            String examineText =
                    (objData.getExamine() != null && !objData.getExamine().trim().isEmpty())
                            ? objData.getExamine()
                            : description; // If no examine text, just use its description.
            String deduceText =
                    (objData.getDeduce() != null && !objData.getDeduce().trim().isEmpty())
                            ? objData.getDeduce()
                            : "You find nothing particularly revealing to deduce about the "
                            + name
                            + "."; // Default no-clue.



            // Create the actual GameObject instance.
            GameObject obj = new GameObject(name, description, examineText, deduceText);

            // Add it to the room. Room's addObject handles storing it by its lowercase name.
            room.addObject(obj);
            objectsLoaded++;
          }
        }
        // If roomData.getObjects() is null, it just means this room has no objects, which is fine.
      } else {
        // Uh oh, the case file defines objects for a room that BuildingExtractor didn't load or
        // couldn't find.
        // This usually means a mismatch in room names between sections of the JSON or an error in
        // BuildingExtractor.
        context.logLoadingMessage(
                "Warning: Room '"
                        + roomData.getName()
                        + "' (defined for objects) not found in context. Objects for this room cannot be loaded.");
        roomNotFoundErrors++;
      }
    }

    // Final summary log. Good for a quick check after loading.
    context.logLoadingMessage(
            "Finished loading game objects. Total Loaded: "
                    + objectsLoaded
                    + ", Invalid Object Data Count: "
                    + invalidObjectDataErrors
                    + // Renamed for clarity
                    ", Room Not Found Count: "
                    + roomNotFoundErrors); // Renamed for clarity
  }
}
