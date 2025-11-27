package common.interfaces;

import Core.Detective;
import Core.DoctorWatson;
import Core.Room;
import Core.Suspect;
import Core.TaskList;
import JsonDTO.CaseData;
import common.dto.JournalEntryDTO;
import common.dto.WatsonHintResponseDTO;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

public interface GameActionContext {

  // --- State Checks & General Info ---
  boolean isCaseStarted();
  void setCaseStarted(boolean started);
  CaseData getSelectedCase();
  Detective getPlayerDetective(String playerId);
  Room getCurrentRoomForPlayer(String playerId);

  // --- World Information & Interaction ---
  String getOccupantsDescriptionInRoom(Room room, String askingPlayerId);
  TaskList getTaskList();
  DoctorWatson getWatson();
  List<Suspect> getAllSuspects();

  // --- Core Player Actions ---
  boolean movePlayer(String playerId, String direction);
  void addJournalEntry(JournalEntryDTO entry);
  List<JournalEntryDTO> getJournalEntries(String playerId);

  // --- Communication (Abstracted for SP/MP) ---
  void sendResponseToPlayer(String playerId, Serializable responseDto);
  void broadcastToSession(Serializable dto, String excludePlayerId);
  void notifyPlayerMove(String movingPlayerId, Room newRoom, Room oldRoom);

  // --- Exam Flow ---
  boolean canStartFinalExam(String playerId);
  void startExamProcess(String playerId);
  void processSubmitQuestionAnswer(String playerId, int questionIndex, Map<String, String> answers);

  // --- NPC and Player Updates ---
  void updateNpcMovements(String triggeringPlayerId);
  void processUpdateDisplayName(String playerId, String newDisplayName);

  // --- Multiplayer/Session Specific Requests ---
  void processRequestStartCase(String requestingPlayerId);
  void processRequestInitiateExam(String requestingPlayerId);
  void handlePlayerExitRequest(String playerId);
  WatsonHintResponseDTO askWatsonForHint(String playerId);
  void handlePlayerCancelLobby(String playerId);

  void processUpdateTaskState(String playerId, int taskIndex, boolean isCompleted);

    /**
     * Gets the total number of deductions used by all players in the session.
     * @return The session-wide deduce count.
     */
    int getSessionDeduceCount();

    /**
     * Increments the session-wide deduce count.
     */
    void incrementSessionDeduceCount();

    void processContinueGame(String playerId);
}
