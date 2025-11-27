package client;

import java.util.List;
import JsonDTO.CaseFile;
import common.dto.PublicGameInfoDTO;
import common.dto.RoomDescriptionDTO;

public interface GameClientStateListener {
  void onDisconnected();
  void onConnecting();
  void onConnected();
  void onMainMenu();
    void onReturnToMainMenu(String message);
  void onHostGameOptions();
  void onCaseSelection(List<CaseFile> cases);
  void onLanguageSelection(CaseFile caseFile);
  void onHostingLobby(String gameCode);
  void onJoinGameOptions();
  void onPublicGamesList(List<PublicGameInfoDTO> games);
  void onPrivateGameEntry();
  void onLobby();
  void onEnterGame(RoomDescriptionDTO initialRoom);
  void onUpdateRoom(RoomDescriptionDTO newRoom);
    void onReceiveCaseInvitation(String invitation, boolean isHost);
    void onJournalUpdated();
    void onChatMessageReceived(common.dto.ChatMessage message);
    void onTaskStateUpdate(int taskIndex, boolean isCompleted);
    void onFinalExamUnlocked();
    void onFinalExamRequest(String requesterDisplayName);
    void onJoinGameFailed(String message);
    void showExamResults(common.dto.ExamResultDTO resultDTO);
}
