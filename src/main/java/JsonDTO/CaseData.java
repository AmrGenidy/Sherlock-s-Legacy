// CREATE THIS NEW FILE: src/main/java/JsonDTO/CaseData.java

package JsonDTO;

import common.dto.FinalExamDTO;

import java.util.List;

/**
 * An interface representing the data for a single, playable case.
 * This contract is implemented by both the raw CaseFile DTO and the
 * single-language LocalizedCaseFile adapter, allowing the game engine
 * to be agnostic about the source of the data.
 */
public interface CaseData {

    String getTitle();
    String getInvitation();
    String getDescription();
    String getStartingRoom();
    List<CaseFile.SuspectData> getSuspects();
    List<CaseFile.RoomData> getRooms();
    FinalExamDTO getFinalExam();
    List<String> getTasks();
    List<String> getWatsonHints();
    List<CaseFile.RankTierData> getRankingTiers();
    String getWinningMessage();

}