// PASTE THIS, REPLACING THE ENTIRE CONTENTS of src/main/java/JsonDTO/CaseFile.java

package JsonDTO;

import com.fasterxml.jackson.annotation.JsonProperty;
import common.dto.FinalExamDTO;

import java.util.List;
import java.util.Map;

public class CaseFile {

  @JsonProperty("universal_title")
  private String universalTitle;
  private String startingRoom;
  private List<RoomData> rooms;
  private Map<String, LocalizedData> localizations;

  // Getters
  public String getUniversalTitle() { return universalTitle; }
  public String getStartingRoom() { return startingRoom; }
  public List<RoomData> getRooms() { return rooms; }
  public Map<String, LocalizedData> getLocalizations() { return localizations; }

  // --- NESTED CLASSES ---
  // Made classes PUBLIC to be accessible from LocalizedCaseFile

  public static class RoomData {
    public String name; // Public for merging
    public String description; // Public for merging
    public Map<String, String> neighbors; // Public for merging
    public List<GameObjectData> objects; // Public for merging

    public String getName() { return name; }
    public String getDescription() { return description; }
    public Map<String, String> getNeighbors() { return neighbors; }
    public List<GameObjectData> getObjects() { return objects; }
  }

  // Renamed from ObjectStub for clarity, matches new structure better
  public static class GameObjectData {
    public String name;
    public String description;
    public String examine;
    public String deduce;

    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getExamine() { return examine; }
    public String getDeduce() { return deduce; }
  }

  public static class LocalizedData {
    public String languageName;
    public String title;
    public String invitation;
    public String description;
    public List<SuspectData> suspects;
    public List<RoomDetailData> roomDetails;
    public List<ObjectDetailData> objectDetails;
    @JsonProperty("final_exam")
    public FinalExamDTO finalExam;
    public List<String> tasks;
    public List<String> watsonHints;
    public List<RankTierData> rankingTiers;
    @JsonProperty("winning_message")
    public String winningMessage;

    // Getters
    public String getLanguageName() { return languageName; }
    public String getTitle() { return title; }
    public String getInvitation() { return invitation; }
    public String getDescription() { return description; }
    public List<SuspectData> getSuspects() { return suspects; }
    public List<RoomDetailData> getRoomDetails() { return roomDetails; }
    public List<ObjectDetailData> getObjectDetails() { return objectDetails; }
    public FinalExamDTO getFinalExam() { return finalExam; }
    public List<String> getTasks() { return tasks; }
    public List<String> getWatsonHints() { return watsonHints; }
    public List<RankTierData> getRankingTiers() { return rankingTiers; }
    public String getWinningMessage() { return winningMessage; }
  }

  public static class SuspectData {
    public String name;
    public String statement;
    public String clue;
    public String getName() { return name; }
    public String getStatement() { return statement; }
    public String getClue() { return clue; }
  }

  public static class RoomDetailData {
    public String name;
    public String description;
    public String getName() { return name; }
    public String getDescription() { return description; }
  }

  public static class ObjectDetailData {
    public String name;
    public String description;
    public String examine;
    public String deduce;
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getExamine() { return examine; }
    public String getDeduce() { return deduce; }
  }

  public static class RankTierData {
    public String rankName;
    public int maxDeductions;
    public String description;
    public boolean defaultRank;
    public String getRankName() { return rankName; }
    public int getMaxDeductions() { return maxDeductions; }
    public String getDescription() { return description; }
    public boolean isDefaultRank() { return defaultRank; }
  }
}