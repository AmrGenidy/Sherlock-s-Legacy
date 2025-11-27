package singleplayer;

import Core.Detective;
import Core.DoctorWatson;
import Core.GameObject;
import Core.Journal;
import Core.Room;
import Core.Rank;
import Core.Suspect;
import Core.TaskList;
import common.dto.ExamQuestionDTO;
import common.dto.ExamResultDTO;
import common.dto.JournalEntryDTO;
import common.dto.RoomDescriptionDTO;
import common.dto.TextMessage;
import common.dto.WatsonHintResponseDTO;
import common.interfaces.GameActionContext;
import common.interfaces.GameContext;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import JsonDTO.CaseData;

public class GameContextSinglePlayer implements GameContext, GameActionContext {

  private static final Logger logger = LoggerFactory.getLogger(GameContextSinglePlayer.class);

  // --- Game State Fields ---
  private Detective detective;
  private DoctorWatson watson;
  private Map<String, Room> rooms;
  private List<Suspect> suspects;
  private Journal<JournalEntryDTO> journal;
  private TaskList taskList;
  private Room currentRoom; // Player's current room
  private boolean caseStarted = false;
  private final Random random = new Random();
  private boolean wantsToExitToCaseSelection = false;
  private boolean wantsToExitApplication = false;
  private common.dto.FinalExamDTO finalExam;
  private Map<Integer, Map<String, String>> playerAnswers;
  private int currentQuestionIndex;
  private boolean isExamActive;
  private ExamResultDTO lastResultDTO;
  private CaseData selectedCase;
  private Map<Integer, Boolean> taskStates;

  public GameContextSinglePlayer() {
    this.detective = new Detective("PlayerDetectiveSP");
    this.rooms = new HashMap<>();
    this.suspects = new ArrayList<>();
    this.journal = new Journal<>();
    this.taskStates = new HashMap<>();
    resetExamState();
  }

  public void resetForNewCaseLoad() {
    logContextMessage("Resetting context for new case load.");
    this.rooms.clear();
    this.suspects.clear();
    this.journal.clearEntries();
    this.taskStates.clear();
    this.taskList = null;
    this.selectedCase = null;
    this.currentRoom = null;
    this.caseStarted = false;
    this.wantsToExitToCaseSelection = false;
    this.wantsToExitApplication = false;
    this.watson = null;
    if (this.detective != null) {
      this.detective.resetForNewCase();
    } else {
      this.detective = new Detective("PlayerDetectiveSP_Fallback");
    }
    resetExamState();
  }

  private void resetExamState() {
    this.isExamActive = false;
    this.finalExam = null;
    this.playerAnswers = null;
    this.currentQuestionIndex = 0;
    this.lastResultDTO = null;
  }

  public void initializeNewCase(CaseData caseFile, String startingRoomName) {
    this.selectedCase = caseFile;
    if (caseFile.getTasks() != null) {
      this.taskList = new TaskList(new ArrayList<>(caseFile.getTasks()));
    } else {
      this.taskList = new TaskList(new ArrayList<>());
    }
    if (caseFile.getWatsonHints() != null && !caseFile.getWatsonHints().isEmpty()) {
      this.watson = new DoctorWatson(new ArrayList<>(caseFile.getWatsonHints()));
    } else {
      this.watson = new DoctorWatson(new ArrayList<>());
    }

    Room startingRoom = getRoomByName(startingRoomName);
    if (startingRoom != null) {
      this.currentRoom = startingRoom;
      this.detective.setCurrentRoom(startingRoom);
      if (this.watson != null) {
        this.watson.setCurrentRoom(startingRoom);
      }
      logContextMessage("Initialized case '" + caseFile.getTitle() + "'. Starting room: " + startingRoom.getName());
    } else {
      logContextMessage("CRITICAL Error: Starting room '" + startingRoomName + "' not found! Cannot set initial position.");
      if (!this.rooms.isEmpty()) {
        this.currentRoom = this.rooms.values().iterator().next();
        this.detective.setCurrentRoom(this.currentRoom);
        if (this.watson != null) this.watson.setCurrentRoom(this.currentRoom);
        logContextMessage("Warning: Using first available room '" + this.currentRoom.getName() + "' as fallback starting room.");
      } else {
        logContextMessage("CRITICAL Error: No rooms loaded at all. Player cannot be placed.");
      }
    }
    if (!this.suspects.isEmpty() && this.suspects.stream().allMatch(s -> s.getCurrentRoom() == null)) {
      logContextMessage("Initializing random suspect positions...");
      if (!this.rooms.isEmpty()) {
        List<Room> allRoomsList = new ArrayList<>(this.rooms.values());
        for (Suspect suspect : this.suspects) {
          suspect.setCurrentRoom(allRoomsList.get(random.nextInt(allRoomsList.size())));
        }
      }
    }
  }


@Override
public void handlePlayerCancelLobby(String playerId) {
    // This command is for multiplayer lobbies.
    // In single player, 'exit' is used to return to case selection,
    // so this method has no action here. We can just provide a log message.
    logContextMessage("handlePlayerCancelLobby called in Single Player context. No action taken.");
    
    // We could potentially have it act like the 'exit' command if we wanted.
    // For now, let's treat it as a command that's not applicable here.
    sendResponseToPlayer(playerId, new TextMessage("The 'cancel' command is not used in this context.", true));
}

  private void broadcastInitialCaseDetailsToPlayer(String playerId) {
    if (selectedCase == null) {
      sendResponseToPlayer(playerId, new TextMessage("Error: No case selected to display details.", true));
      return;
    }
    sendResponseToPlayer(playerId, new TextMessage("--- Case Description ---\n" + selectedCase.getDescription(), false));
    if (taskList != null && !taskList.getTasks().isEmpty()) {
      StringBuilder taskMessage = new StringBuilder("--- Case Tasks ---\n");
      List<String> tasks = taskList.getTasks();
      for (int i = 0; i < tasks.size(); i++) {
        taskMessage.append((i + 1)).append(". ").append(tasks.get(i)).append("\n");
      }
      sendResponseToPlayer(playerId, new TextMessage(taskMessage.toString().trim(), false));
    } else {
      sendResponseToPlayer(playerId, new TextMessage("No tasks available for this case.", false));
    }

    List<JsonDTO.CaseFile.RankTierData> tiers = selectedCase.getRankingTiers();
    if (tiers != null && !tiers.isEmpty()) {
        StringBuilder rankMessage = new StringBuilder("\n--- Rank Evaluation ---\n");
        rankMessage.append("Your final rank will be determined by the number of 'deduce' commands used:\n");
    
        tiers.sort(Comparator.comparingInt(JsonDTO.CaseFile.RankTierData::getMaxDeductions));
        
        // Use a final, single-element array as a mutable container
        final int[] lastMax = { -1 }; // Start at -1 to handle a range starting at 0
        String rangePrefix = "  - ";
    
        for(JsonDTO.CaseFile.RankTierData tier : tiers){
            if (tier.isDefaultRank()) continue;
    
            int lowerBound = lastMax[0] + 1;
            int upperBound = tier.getMaxDeductions();
            String range;
            
            if(lowerBound > upperBound) continue;
            if (lowerBound == upperBound) {
                range = String.valueOf(lowerBound);
            } else {
                range = lowerBound + "-" + upperBound;
            }
            
            rankMessage.append(String.format("%s%-20s: %s deductions\n", rangePrefix, tier.getRankName(), range));
            lastMax[0] = upperBound; // Modify the content of the array
        }
    
        // This lambda can now safely access the final 'lastMax' array reference
        tiers.stream().filter(JsonDTO.CaseFile.RankTierData::isDefaultRank).findFirst().ifPresent(tier -> {
            rankMessage.append(String.format("%s%-20s: %d+ deductions\n", rangePrefix, tier.getRankName(), lastMax[0] + 1));
        });
        
        sendResponseToPlayer(playerId, new TextMessage(rankMessage.toString().trim(), false));
    }

    Room spCurrentRoom = getCurrentRoomForPlayer(playerId);
    if (spCurrentRoom != null) {
      sendResponseToPlayer(playerId, new TextMessage("\nYou are now at the starting location: " + spCurrentRoom.getName(), false));
      sendResponseToPlayer(playerId, createRoomDescriptionDTO(spCurrentRoom, playerId));
    } else {
      sendResponseToPlayer(playerId, new TextMessage("Error: Starting location not found or player not placed.", true));
    }
    sendResponseToPlayer(playerId, new TextMessage("\nType 'help' to see available commands.", false));
  }

  private void logContextMessage(String message) {
    logger.info(message);
}

  public RoomDescriptionDTO createRoomDescriptionDTO(Room room, String playerId) {
    if (room == null) return null;
    List<String> objectNames = room.getObjects().values().stream().map(GameObject::getName).collect(Collectors.toList());
    String occupantsStr = getOccupantsDescriptionInRoom(room, playerId);
    List<String> occupantNamesList = new ArrayList<>();
    if (occupantsStr != null && !occupantsStr.equalsIgnoreCase("Occupants: None") && occupantsStr.startsWith("Occupants: ")) {
      String[] names = occupantsStr.substring("Occupants: ".length()).split(",\\s*");
      for (String name : names) {
        if (!name.trim().isEmpty()) occupantNamesList.add(name.trim());
      }
    }
    Map<String, String> exits = room.getNeighbors().entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().getName()));
    return new RoomDescriptionDTO(room.getName(), room.getDescription(), objectNames, occupantNamesList, exits);
  }

  // --- GameContext Implementation (for Extractors) ---
  @Override
  public void addRoom(Room room) {
    if (room != null && room.getName() != null) {
      this.rooms.put(room.getName().toLowerCase(), room);
    } else {
      logContextMessage("Warning: Attempted to add null room or room with null name.");
    }
  }

  @Override
  public Room getRoomByName(String name) {
    if (name == null) return null;
    return this.rooms.get(name.toLowerCase());
  }

  @Override
  public Map<String, Room> getAllRooms() {
    return Collections.unmodifiableMap(this.rooms);
  }

  @Override
  public void addSuspect(Suspect suspect) {
    if (suspect != null) {
      this.suspects.add(suspect);
    } else {
      logContextMessage("Warning: Attempted to add null suspect.");
    }
  }

  @Override
  public void logLoadingMessage(String message) {
      logger.info("[LOADER_SP] {}", message);
  }

  @Override
  public String getContextIdForLog() {
    return "SinglePlayer";
  }

  // --- GameActionContext Implementation (for Commands) ---
  @Override
  public boolean isCaseStarted() {
    return this.caseStarted;
  }

  @Override
  public void setCaseStarted(boolean started) {
    if (this.caseStarted == started) {
      logContextMessage("setCaseStarted(" + started + ") called, but state is already " + this.caseStarted);
      return;
    }
    this.caseStarted = started;
    if (started) {
      logContextMessage("Case '" + (selectedCase != null ? selectedCase.getTitle() : "Unknown") + "' has been started.");
      broadcastInitialCaseDetailsToPlayer(this.detective.getPlayerId());
    } else {
      logContextMessage("Case '" + (selectedCase != null ? selectedCase.getTitle() : "Unknown") + "' has been stopped/reset.");
    }
  }

  @Override
  public CaseData getSelectedCase() { // MODIFIED: Return type is CaseData
    return this.selectedCase;
  }

    public CaseData getCaseFile() {
        return this.selectedCase;
    }

    @Override
  public Detective getPlayerDetective(String playerId) {
    return this.detective;
  }

  @Override
  public Room getCurrentRoomForPlayer(String playerId) {
    return this.currentRoom;
  }

  @Override
  public String getOccupantsDescriptionInRoom(Room room, String askingPlayerId) {
    if (room == null) {
      logContextMessage("Error: getOccupantsDescriptionInRoom called with null room.");
      return "Occupants: Error";
    }
    List<String> occupantNames = new ArrayList<>();
    for (Suspect suspect : this.suspects) {
      if (suspect.getCurrentRoom() != null && suspect.getCurrentRoom().getName().equalsIgnoreCase(room.getName())) {
        occupantNames.add(suspect.getName());
      }
    }
    if (this.watson != null && this.watson.getCurrentRoom() != null && this.watson.getCurrentRoom().getName().equalsIgnoreCase(room.getName())) {
      occupantNames.add("Dr. Watson");
    }
    return occupantNames.isEmpty() ? "Occupants: None" : "Occupants: " + String.join(", ", occupantNames);
  }

  @Override
  public TaskList getTaskList() {
    return this.taskList;
  }

  @Override
  public DoctorWatson getWatson() {
    return this.watson;
  }

  @Override
  public List<Suspect> getAllSuspects() {
    return Collections.unmodifiableList(this.suspects);
  }

  @Override
  public boolean movePlayer(String playerId, String direction) {
    if (this.currentRoom == null) {
      sendResponseToPlayer(playerId, new TextMessage("Error: Cannot move, current location unknown.", true));
      return false;
    }
    Room oldPlayerRoom = this.currentRoom;
    Room newPlayerRoom = oldPlayerRoom.getNeighbor(direction.toLowerCase());
    if (newPlayerRoom != null) {
      this.currentRoom = newPlayerRoom;
      this.detective.setCurrentRoom(newPlayerRoom);
      updateNpcMovements(playerId);
      sendResponseToPlayer(playerId, createRoomDescriptionDTO(newPlayerRoom, playerId));
      return true;
    } else {
      sendResponseToPlayer(playerId, new TextMessage("You can't move " + direction + " from " + oldPlayerRoom.getName() + ".", false));
      return false;
    }
  }

  @Override
  public void addJournalEntry(JournalEntryDTO entry) {
    if (entry == null) {
      logContextMessage("Attempted to add a null entry to the journal.");
      return;
    }
    if (this.journal.addEntry(entry)) {
      logContextMessage("Journal entry added by " + entry.getContributorPlayerId() + ". Journal size now: " + this.journal.getEntryCount());
      sendResponseToPlayer(entry.getContributorPlayerId(), entry);
    } else {
      logContextMessage("Journal entry by " + entry.getContributorPlayerId() + " was considered a duplicate, not added. Journal size: " + this.journal.getEntryCount());
    }
  }

  @Override
  public List<JournalEntryDTO> getJournalEntries(String playerId) {
    List<JournalEntryDTO> entriesFromJournal = this.journal.getEntries();
    return new ArrayList<>(entriesFromJournal).stream()
            .sorted(Comparator.comparingLong(JournalEntryDTO::getTimestamp))
            .collect(Collectors.toList());
  }

  @Override
  public void sendResponseToPlayer(String playerId, Serializable responseDto) {
    if (responseDto == null) return;
    String output;
    if (responseDto instanceof TextMessage) {
      String text = ((TextMessage) responseDto).getText();
      if (text.contains("--- Final Exam Started ---")) {
          output = "[EXAM_STARTED] " + text;
      } else {
          output = text;
      }
    } else if (responseDto instanceof RoomDescriptionDTO rd) {
      StringBuilder sb = new StringBuilder();
      // Add a machine-readable tag for the output parser
      sb.append("[ROOM_UPDATE]\n");
      sb.append("--- ").append(rd.getName()).append(" ---\n");
      sb.append(rd.getDescription()).append("\n");
      sb.append("Objects: ").append(rd.getObjectNames().isEmpty() ? "None" : String.join(", ", rd.getObjectNames())).append("\n");
      sb.append("Occupants: ").append(rd.getOccupantNames().isEmpty() ? "None" : String.join(", ", rd.getOccupantNames())).append("\n");
      sb.append("Exits: ");
      if (rd.getExits().isEmpty()) {
        sb.append("None");
      } else {
        rd.getExits().forEach((dir, roomName) -> sb.append(dir).append(" (to ").append(roomName).append("), "));
        if (!rd.getExits().isEmpty()) sb.setLength(sb.length() - 2);
      }
      output = sb.toString();
    } else if (responseDto instanceof JournalEntryDTO) {
      output = "[JOURNAL UPDATE] " + responseDto;
    } else if (responseDto instanceof ExamQuestionDTO examQuestion) {
        StringBuilder sb = new StringBuilder();
        sb.append("[EXAM_QUESTION]\n");
        sb.append("\n--- EXAM QUESTION ").append(examQuestion.getQuestionIndex() + 1).append(" of ").append(examQuestion.getTotalQuestions()).append(" ---\n");
        sb.append(examQuestion.getQuestionPrompt()).append("\n");
        for (Map.Entry<String, common.dto.FinalExamSlotDTO> entry : examQuestion.getSlots().entrySet()) {
            sb.append("\n").append(entry.getKey()).append(" choices:\n");
            for (int i = 0; i < entry.getValue().getChoices().size(); i++) {
                sb.append("  ").append(i + 1).append(") ").append(entry.getValue().getChoices().get(i).getChoiceText()).append("\n");
            }
        }
        sb.append("\nEnter your choices (e.g., '1,2'):");
        output = sb.toString();
    } else if (responseDto instanceof ExamResultDTO) {
      output = "[EXAM_RESULT]\n" + responseDto.toString();
    } else {
      output = "[SP_RESPONSE] " + responseDto;
    }
    System.out.println(output);
  }

  @Override
  public void broadcastToSession(Serializable dto, String excludePlayerId) {
    sendResponseToPlayer(null, dto);
  }

  @Override
  public void notifyPlayerMove(String movingPlayerId, Room newRoom, Room oldRoom) {
    // No action needed in SP.
  }

  @Override
  public boolean canStartFinalExam(String playerId) {
    return isCaseStarted() && !isExamActive;
  }

  @Override
  public void startExamProcess(String playerId) {
    if (!canStartFinalExam(playerId)) {
      sendResponseToPlayer(playerId, new TextMessage("Cannot start the final exam now.", true));
      return;
    }
    startExamForPlayer(playerId);
  }

  public void startExamForPlayer(String playerId) {
    if (this.selectedCase == null || this.selectedCase.getFinalExam() == null || this.selectedCase.getFinalExam().getQuestions().isEmpty()) {
      sendResponseToPlayer(playerId, new TextMessage("No final exam questions are configured for this case.", false));
      return;
    }
    this.isExamActive = true;
    this.finalExam = this.selectedCase.getFinalExam();
    this.playerAnswers = new HashMap<>();
    this.currentQuestionIndex = 0;
    sendResponseToPlayer(playerId, new TextMessage("--- Final Exam Started ---", false));
    sendNextQuestionToPlayer(playerId);
  }

  public boolean isExamActive() {
      return isExamActive;
  }

  public ExamQuestionDTO getCurrentExamQuestionDTO() {
      if (!isExamActive || finalExam == null || currentQuestionIndex >= finalExam.getQuestions().size()) return null;
      common.dto.FinalExamQuestionDTO currentQ = finalExam.getQuestions().get(currentQuestionIndex);
      Map<String, String> selectedAnswers = playerAnswers.getOrDefault(currentQuestionIndex, new HashMap<>());
      return new ExamQuestionDTO(currentQuestionIndex, finalExam.getQuestions().size(), currentQ.getQuestionPrompt(), currentQ.getSlots(), selectedAnswers);
  }

  public ExamResultDTO getLastResultDTO() {
      return lastResultDTO;
  }

  private void sendNextQuestionToPlayer(String playerId) {
    if (!isExamActive) return;
    if (currentQuestionIndex < finalExam.getQuestions().size()) {
      sendResponseToPlayer(playerId, getCurrentExamQuestionDTO());
    } else {
      evaluateAndSendExamResults(playerId);
    }
  }

private void evaluateAndSendExamResults(String playerId) {
  if (finalExam == null || playerAnswers == null) {
      sendResponseToPlayer(playerId, new TextMessage("Error: Exam data missing for evaluation.", true));
      resetExamState();
      return;
  }

  int score = 0;
  List<String> answersToReviewDetails = new ArrayList<>();
  int totalQuestions = finalExam.getQuestions().size();

    for (int i = 0; i < totalQuestions; i++) {
        common.dto.FinalExamQuestionDTO actualQuestion = finalExam.getQuestions().get(i);
        Map<String, String> correctCombination = actualQuestion.getCorrectCombination();
        Map<String, String> playerAnswer = playerAnswers.get(i);

        if (playerAnswer != null && playerAnswer.equals(correctCombination)) {
            score++;
        } else {
            String reviewDetail = String.format("Q%d: %s\n   Your Answer: '%s'", (i + 1), actualQuestion.getQuestionPrompt(), (playerAnswer != null ? playerAnswer : "No answer provided"));
            answersToReviewDetails.add(reviewDetail);
        }
    }

  this.detective.setFinalExamScore(score);
  
  // --- REVISED RANK EVALUATION FOR SINGLE PLAYER ---
  // In SP, the "session count" is just the detective's personal count.
  Rank finalRank = Core.util.RankEvaluator.evaluate(this.detective.getDeduceCount(), this.selectedCase);
  this.detective.setRank(finalRank);
  
  String finalRankString = "Unranked";
  if (finalRank != null) {
      finalRankString = finalRank.getRankName();
      // Append the rank's flavor text for a nice SP end screen.
      finalRankString += "\n(" + finalRank.getDescription() + ")";
  }
  // --- END REVISED RANK EVALUATION ---

  String feedbackMessage;
  if (score == totalQuestions) {
      feedbackMessage = "Outstanding! You've answered all questions correctly and solved the case perfectly.";
  } else if (score >= totalQuestions * 0.5) {
      feedbackMessage = "You've made some progress. Review your notes and the evidence for the questions you missed.";
  } else {
      feedbackMessage = "Unfortunately, your investigation fell short. Crucial details were missed. Review your notes and the evidence thoroughly.";
  }

  boolean caseSolved = (score == totalQuestions);
  String winningMessage = (caseSolved && this.selectedCase != null) ? this.selectedCase.getWinningMessage() : null;

  this.lastResultDTO = new ExamResultDTO(score, totalQuestions, feedbackMessage, finalRankString, answersToReviewDetails, winningMessage, caseSolved);

  sendResponseToPlayer(playerId, lastResultDTO);
  sendResponseToPlayer(playerId, new TextMessage("--- Final Exam Concluded ---", false));
  sendResponseToPlayer(playerId, new TextMessage("1. Continue Playing", false));
  if (caseSolved) {
      sendResponseToPlayer(playerId, new TextMessage("2. Return to Main Menu", false));
  }

  resetExamState();
}

  @Override
  public void updateNpcMovements(String triggeringPlayerId) {
    if (!caseStarted) return;
    if (this.currentRoom == null) return;

    for (Suspect suspect : this.suspects) {
      if (suspect.getCurrentRoom() == null) continue;
      Map<String, Room> neighbors = suspect.getCurrentRoom().getNeighbors();
      if (neighbors.isEmpty()) continue;
      List<Room> allPossibleMoves = new ArrayList<>(neighbors.values());
      if (!allPossibleMoves.isEmpty()) {
        Room nextRoomForSuspect = allPossibleMoves.get(random.nextInt(allPossibleMoves.size()));
        suspect.setCurrentRoom(nextRoomForSuspect);
      }
    }
    if (this.watson != null && this.watson.getCurrentRoom() != null) {
      Map<String, Room> watsonNeighbors = this.watson.getCurrentRoom().getNeighbors();
      if (!watsonNeighbors.isEmpty()) {
        List<Room> watsonPossibleMoves = new ArrayList<>(watsonNeighbors.values());
        Room nextRoomForWatson = watsonPossibleMoves.get(random.nextInt(watsonPossibleMoves.size()));
        this.watson.setCurrentRoom(nextRoomForWatson);
      }
    }
  }

  @Override
  public void handlePlayerExitRequest(String playerId) {
    // In single-player, "exit" should always go back to the case selection, never exit the app.
    sendResponseToPlayer(playerId, new TextMessage("Exiting current case. Returning to case selection.", false));
    resetExamState();
    this.caseStarted = false;
    this.wantsToExitToCaseSelection = true;
  }

  @Override
  public void processUpdateDisplayName(String playerId, String newDisplayName) {
    Detective spDetective = getPlayerDetective(playerId);
    if (spDetective != null) {
      logContextMessage("Display name update processed for " + spDetective.getPlayerId() + " to " + newDisplayName + ". (In SP, client handles its own display name for prompts).");
      sendResponseToPlayer(playerId, new TextMessage("Display name noted as: " + newDisplayName, false));
    } else {
      logContextMessage("Error: processUpdateDisplayName called, but SP detective not found for ID: " + playerId);
    }
  }

  @Override
  public void processRequestStartCase(String requestingPlayerId) {
    logContextMessage("Received 'request start case' in Single Player for player: " + requestingPlayerId);
    if (isCaseStarted()) {
      sendResponseToPlayer(requestingPlayerId, new TextMessage("The case has already started.", false));
      return;
    }
    if (getSelectedCase() == null) {
      sendResponseToPlayer(requestingPlayerId, new TextMessage("No case selected to start.", true));
      return;
    }
    setCaseStarted(true);
  }

  @Override
  public void processRequestInitiateExam(String requestingPlayerId) {
    logContextMessage("Received 'request final exam' in Single Player for player: " + requestingPlayerId);
    if (!isCaseStarted()) {
      sendResponseToPlayer(requestingPlayerId, new TextMessage("The case has not started yet. Cannot start exam.", true));
      return;
    }
    if (isExamActive) {
      sendResponseToPlayer(requestingPlayerId, new TextMessage("An exam is already in progress.", false));
      return;
    }
    startExamProcess(requestingPlayerId);
  }

  @Override
  public WatsonHintResponseDTO askWatsonForHint(String playerId) {
    if (this.watson == null) {
      return new WatsonHintResponseDTO("Dr. Watson is not available in this case.", false);
    }
    if (this.currentRoom == null) {
      return new WatsonHintResponseDTO("Your location is unknown. Cannot determine if Watson is present.", false);
    }
    if (this.watson.getCurrentRoom() == null) {
      return new WatsonHintResponseDTO("Dr. Watson's location is currently unknown.", false);
    }
    if (this.watson.getCurrentRoom().getName().equalsIgnoreCase(this.currentRoom.getName())) {
      String hintText = this.watson.provideHint();
      boolean isActualGameHint = true;
      if (hintText == null || hintText.trim().isEmpty() ||
              hintText.startsWith("I seem to be out of specific thoughts") ||
              hintText.startsWith("My mind is blank") ||
              hintText.startsWith("I'm afraid I have no specific insights")) {
        isActualGameHint = false;
      }
      if (hintText == null || hintText.trim().isEmpty()) {
        hintText = "Dr. Watson ponders but offers no specific insight at the moment.";
      }
      return new WatsonHintResponseDTO(hintText, isActualGameHint);
    } else {
      return new WatsonHintResponseDTO("Dr. Watson is not in this room.", false);
    }
  }

  // --- SP Specific Flow Control ---
  public boolean wantsToExitToCaseSelection() {
    return wantsToExitToCaseSelection;
  }

  public boolean wantsToExitApplication() {
    return wantsToExitApplication;
  }

  public void resetExitFlags() {
    this.wantsToExitApplication = false;
    this.wantsToExitToCaseSelection = false;
  }

  public boolean isAwaitingExamAnswer() {
    return isExamActive && finalExam != null && currentQuestionIndex < finalExam.getQuestions().size();
  }

  public int getAwaitingQuestionNumber() {
    return isAwaitingExamAnswer() ? currentQuestionIndex + 1 : 0;
  }


  @Override
  public int getSessionDeduceCount() {
      return (this.detective != null) ? this.detective.getDeduceCount() : 0;
  }
  
  @Override
  public void incrementSessionDeduceCount() {
      // The DeduceCommand will call detective.incrementDeduceCount() directly,
      // which is the single source of truth for SP. This method does nothing.
  }

  @Override
  public void processUpdateTaskState(String playerId, int taskIndex, boolean isCompleted) {
    if (taskList != null && taskIndex >= 0 && taskIndex < taskList.getTasks().size()) {
      taskStates.put(taskIndex, isCompleted);
      logContextMessage(
          "Task " + taskIndex + " state updated to: " + (isCompleted ? "Completed" : "Incomplete"));
      // In SP, the UI state is managed centrally, so we don't need to broadcast.
      // A confirmation message isn't necessary as the UI change is immediate.
    } else {
      logContextMessage(
          "Warning: Invalid task index received for update: " + taskIndex);
    }
  }


    @Override
    public void processSubmitQuestionAnswer(String playerId, int questionIndex, Map<String, String> answers) {
        if (!isExamActive || finalExam == null) {
            sendResponseToPlayer(playerId, new TextMessage("No exam is currently active.", true));
            return;
        }

        if (questionIndex != currentQuestionIndex) {
            return;
        }

        playerAnswers.put(questionIndex, answers);

        if (currentQuestionIndex < finalExam.getQuestions().size() - 1) {
            currentQuestionIndex++;
            sendNextQuestionToPlayer(playerId);
        } else {
            evaluateAndSendExamResults(playerId);
        }
    }

    @Override
    public void processContinueGame(String playerId) {
        if (currentRoom == null) return;
        // In Single Player, "Continue" just means showing the current room again (like a 'look' command)
        // to confirm they are back in the game world.
        sendResponseToPlayer(playerId, createRoomDescriptionDTO(currentRoom, playerId));
    }
}