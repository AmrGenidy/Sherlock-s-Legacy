package JsonDTO;

import common.dto.FinalExamDTO;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * An adapter class that represents a single-language version of a CaseFile.
 * It's constructed from a multilingual CaseFile DTO and a specific language code.
 * It provides the exact same getters as the OLD CaseFile DTO, so the rest of the
 * game engine (Extractors, Contexts) doesn't need to be changed.
 */
public class LocalizedCaseFile implements CaseData {

    // These fields mirror the OLD CaseFile structure
    private String title;
    private String invitation;
    private String description;
    private String startingRoom;
    private List<CaseFile.SuspectData> suspects;
    private List<CaseFile.RoomData> rooms;
    private FinalExamDTO finalExam;
    private List<String> tasks;
    private List<String> watsonHints;
    private List<CaseFile.RankTierData> rankingTiers;
    private String winningMessage;

    /**
     * Constructs a single-language case file from a multilingual source.
     * @param multiLingualCase The fully parsed CaseFile DTO containing all languages.
     * @param languageCode The language to extract (e.g., "en", "es").
     */
    public LocalizedCaseFile(CaseFile multiLingualCase, String languageCode) {
        CaseFile.LocalizedData locData = multiLingualCase.getLocalizations().get(languageCode);
        if (locData == null) {
            // Fallback to the first available language if the chosen one doesn't exist
            String fallbackCode = multiLingualCase.getLocalizations().keySet().iterator().next();
            locData = multiLingualCase.getLocalizations().get(fallbackCode);
            System.err.println("Warning: Language '" + languageCode + "' not found. Falling back to '" + fallbackCode + "'.");
        }

        // 1. Populate simple text fields
        this.title = locData.getTitle();
        this.invitation = locData.getInvitation();
        this.description = locData.getDescription();
        this.finalExam = locData.getFinalExam();
        this.tasks = locData.getTasks();
        this.watsonHints = locData.getWatsonHints();
        this.rankingTiers = locData.getRankingTiers();
        this.winningMessage = locData.getWinningMessage();

        // 2. Populate fields from the top-level structure
        this.startingRoom = multiLingualCase.getStartingRoom();
        this.suspects = locData.getSuspects(); // Suspects are fully defined in localized data

        // 3. Merge logical room structure with localized text details
        Map<String, String> roomDescriptions = locData.getRoomDetails().stream()
                .collect(Collectors.toMap(CaseFile.RoomDetailData::getName, CaseFile.RoomDetailData::getDescription));

        Map<String, CaseFile.ObjectDetailData> objectDetails = locData.getObjectDetails().stream()
                .collect(Collectors.toMap(CaseFile.ObjectDetailData::getName, detail -> detail));

        this.rooms = multiLingualCase.getRooms().stream()
                .map(logicalRoom -> {
                    // Create a new RoomData that will hold the merged info
                    CaseFile.RoomData localizedRoom = new CaseFile.RoomData();
                    localizedRoom.name = logicalRoom.getName();
                    localizedRoom.neighbors = logicalRoom.getNeighbors();
                    // Get the description from our localized map
                    localizedRoom.description = roomDescriptions.getOrDefault(logicalRoom.getName(), "A non-descript room.");

                    // Now merge the object details
                    if (logicalRoom.getObjects() != null) {
                        localizedRoom.objects = logicalRoom.getObjects().stream()
                                .map(objectStub -> {
                                    CaseFile.ObjectDetailData details = objectDetails.get(objectStub.getName());
                                    // Create the full object data DTO the extractors expect
                                    CaseFile.GameObjectData fullObject = new CaseFile.GameObjectData();
                                    fullObject.name = objectStub.getName();
                                    if (details != null) {
                                        fullObject.description = details.getDescription();
                                        fullObject.examine = details.getExamine();
                                        fullObject.deduce = details.getDeduce();
                                    }
                                    return fullObject;
                                }).collect(Collectors.toList());
                    }
                    return localizedRoom;
                }).collect(Collectors.toList());
    }

    // GETTERS (These must match the getters from your OLD CaseFile DTO)
    public String getTitle() { return title; }
    public String getInvitation() { return invitation; }
    public String getDescription() { return description; }
    public String getStartingRoom() { return startingRoom; }
    public List<CaseFile.SuspectData> getSuspects() { return suspects; }
    public List<CaseFile.RoomData> getRooms() { return rooms; }
    public FinalExamDTO getFinalExam() { return finalExam; }
    public List<String> getTasks() { return tasks; }
    public List<String> getWatsonHints() { return watsonHints; }
    public List<CaseFile.RankTierData> getRankingTiers() { return rankingTiers; }
    public String getWinningMessage() { return winningMessage; }
}