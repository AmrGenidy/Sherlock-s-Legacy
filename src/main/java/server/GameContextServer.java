package server;

import Core.*;
import Core.util.RankEvaluator;
import JsonDTO.CaseFile;
import common.commands.Command;
import common.commands.InitiateFinalExamCommand;
import common.commands.StartCaseCommand;
import common.commands.UpdateTaskStateCommand;
import common.dto.*;
import common.interfaces.GameActionContext;
import common.interfaces.GameContext;
import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import JsonDTO.CaseData;

public class GameContextServer implements GameContext, GameActionContext {

  private static final Logger logger = LoggerFactory.getLogger(GameContextServer.class);

  private final GameSession gameSession; // Reference back to the session for communication
  // Player specific state - managed by player IDs
  private Detective player1Detective;
  private Detective player2Detective;
  private String player1Id; // Host
  private String player2Id; // Guest

  // Shared game state
  private Map<String, Room> rooms;
  private List<Suspect> suspects;
  private DoctorWatson watson;
  private Journal<JournalEntryDTO> journal;
  private TaskList taskList;
  private boolean caseStarted = false;
  private final Random random = new Random();

  // Session-wide tracking for multiplayer mechanics
  private Set<String> deducedObjectsInSession; // Names of objects deduced by anyone in this session
  private int sessionDeduceCount;

  // Exam state for the session
  private boolean examActiveForSession = false;
  private FinalExamDTO finalExam;
  private Map<Integer, Map<String, String>> playerAnswers; // Key: Question Index, Value: Map<SlotID, ChoiceID>
  private int currentExamQuestionIndex;
  private final CaseData selectedCase;

  // Shared task state for the session
  private Map<Integer, Boolean> taskStates; // <task index, isCompleted>



  public GameContextServer(
          GameSession gameSession, CaseData selectedCase, String p1Id, String p2Id) {
    this.gameSession = Objects.requireNonNull(gameSession, "GameSession cannot be null");
    this.selectedCase = Objects.requireNonNull(selectedCase, "SelectedCase (CaseData) cannot be null");
    // Player IDs can be null initially if P2 hasn't joined when context is first created by
    // GameSession constructor
    this.player1Id = p1Id;
    this.player2Id = p2Id;

    resetForNewCaseLoad(); // Initialize all collections and states

    if (p1Id != null) this.player1Detective = new Detective(p1Id);
    if (p2Id != null) this.player2Detective = new Detective(p2Id);
    this.taskStates = new HashMap<>();
  }

  // Called by GameSession when P2 joins or if context needs re-init with both players
  public void setPlayerIds(String p1Id, String p2Id) {
    this.player1Id = p1Id;
    this.player2Id = p2Id;

    if (p1Id != null) {
      if (this.player1Detective == null || !this.player1Detective.getPlayerId().equals(p1Id)) {
        this.player1Detective = new Detective(p1Id);
      }
    } else {
      this.player1Detective = null;
    }

    if (p2Id != null) {
      if (this.player2Detective == null || !this.player2Detective.getPlayerId().equals(p2Id)) {
        this.player2Detective = new Detective(p2Id);
      }
      // *** ADDED/MODIFIED: Ensure P2's room is set if game is ready ***
      if (this.selectedCase != null
              && this.selectedCase.getStartingRoom() != null
              && this.player2Detective != null) {
        Room startingRoom = getRoomByName(this.selectedCase.getStartingRoom());
        if (startingRoom != null) {
          this.player2Detective.setCurrentRoom(startingRoom);
          logGameMessage(
                  "Player 2 (" + p2Id + ") position set to starting room: " + startingRoom.getName());
        } else {
          logGameMessage(
                  "Warning: Could not set starting room for Player 2 ("
                          + p2Id
                          + ") upon ID set - starting room not found.");
        }
      }
    } else {
      this.player2Detective = null;
    }
    // If initializePlayerStartingState() is robust enough to handle being called multiple times
    // or if it checks if players already have rooms, you could call it here.
    // For now, direct setting is more targeted.
    // initializePlayerStartingState(); // Re-evaluate if this is needed here
  }

  public void resetForNewCaseLoad() {
    this.rooms = new HashMap<>();
    this.suspects = new ArrayList<>();
    this.journal = new Journal<>();
    this.deducedObjectsInSession = new HashSet<>();
    this.sessionDeduceCount = 0;
    this.caseStarted = false;
    this.examActiveForSession = false;
    this.finalExam = null;
    this.playerAnswers = null;
    this.currentExamQuestionIndex = 0;

    if (selectedCase.getTasks() != null) {
      this.taskList = new TaskList(new ArrayList<>(selectedCase.getTasks()));
    } else {
      this.taskList = new TaskList(new ArrayList<>());
      logGameMessage("Warning: No tasks found in selected case '" + selectedCase.getTitle() + "'.");
    }
    if (selectedCase.getWatsonHints() != null && !selectedCase.getWatsonHints().isEmpty()) {
      this.watson = new DoctorWatson(new ArrayList<>(selectedCase.getWatsonHints()));
    } else {
      this.watson = new DoctorWatson(new ArrayList<>()); // Watson with no hints
      logGameMessage(
              "Warning: No Watson hints found in selected case '" + selectedCase.getTitle() + "'.");
    }

    if (player1Detective != null) player1Detective.resetForNewCase();
    if (player2Detective != null) player2Detective.resetForNewCase();
  }

  public void initializePlayerStartingState() {
    if (selectedCase.getStartingRoom() == null) {
      logGameMessage(
              "CRITICAL Error: Cannot initialize player state, selected case has no startingRoom defined.");
      gameSession.endSession(
              "Configuration error: No starting room defined."); // End session if critical
      return;
    }
    Room startingRoom = getRoomByName(selectedCase.getStartingRoom());
    if (startingRoom == null) {
      logGameMessage(
              "CRITICAL Error: Starting room '"
                      + selectedCase.getStartingRoom()
                      + "' (defined in case) not found after loading rooms.");
      // Attempt to use any room as a fallback, but this is a severe config issue.
      if (!rooms.isEmpty()) {
        startingRoom = rooms.values().iterator().next();
        logGameMessage(
                "Warning: Using first available room '"
                        + startingRoom.getName()
                        + "' as fallback starting room.");
      } else {
        logGameMessage("CRITICAL Error: No rooms loaded at all. Cannot set starting room.");
        gameSession.endSession("Configuration error: No rooms loaded."); // End session
        return;
      }
    }

    logGameMessage("Initializing player states. Starting room: " + startingRoom.getName());
    if (player1Detective != null) {
      player1Detective.resetForNewCase();
      player1Detective.setCurrentRoom(startingRoom);
    }
    if (player2Detective != null) {
      player2Detective.resetForNewCase();
      player2Detective.setCurrentRoom(startingRoom);
    }
    if (watson != null) {
      watson.setCurrentRoom(startingRoom);
    }

    // Initialize suspect positions
    if (!this.suspects.isEmpty() && !this.rooms.isEmpty()) {
      List<Room> allRoomsList = new ArrayList<>(this.rooms.values());
      final Room finalStartingRoom = startingRoom; // For lambda
      for (Suspect suspect : this.suspects) {
        List<Room> validSuspectStarts =
                allRoomsList.stream()
                        .filter(
                                r ->
                                        !r.getName()
                                                .equalsIgnoreCase(
                                                        finalStartingRoom.getName())) // Try not to start in player room
                        .collect(Collectors.toList());
        if (!validSuspectStarts.isEmpty()) {
          suspect.setCurrentRoom(validSuspectStarts.get(random.nextInt(validSuspectStarts.size())));
        } else { // Fallback if only one room or all rooms are starting room
          suspect.setCurrentRoom(allRoomsList.get(random.nextInt(allRoomsList.size())));
        }
      }
    }
  }

  private void logGameMessage(String message) {
    // This now uses the GameSession's getSessionId() to add context to the log message.
    logger.info("[SESS_CTX:{}] {}", gameSession.getSessionId(), message);
  }

  // --- GameContext Implementation (for Extractors) ---
  @Override
  public void addRoom(Room room) {
    if (room != null && room.getName() != null) rooms.put(room.getName().toLowerCase(), room);
    else logGameMessage("Error: Attempted to add null room or room with null name.");
  }

  @Override
  public Room getRoomByName(String name) {
    return name != null ? rooms.get(name.toLowerCase()) : null;
  }

  @Override
  public Map<String, Room> getAllRooms() {
    return Collections.unmodifiableMap(rooms);
  }

  @Override
  public void addSuspect(Suspect suspect) {
    if (suspect != null) suspects.add(suspect);
    else logGameMessage("Error: Attempted to add null suspect.");
  }

  @Override
  public void logLoadingMessage(String message) {
    // We can differentiate loader messages with a specific marker
    logger.info("[LOADER] {}", message);
  }

  @Override
  public String getContextIdForLog() {
    return "ServerSess-" + gameSession.getSessionId();
  }

  // --- GameActionContext Implementation (for Commands) ---
  @Override
  public boolean isCaseStarted() {
    return caseStarted;
  }

  // Inside server.GameContextServer.java

  @Override
  public void setCaseStarted(boolean started) {
    // Prevent re-entry or redundant calls if state is already set
    if (this.caseStarted == started && started) {
      logGameMessage("setCaseStarted(true) called, but case was already started.");
      // If already started, perhaps resend a minimal "game is active" confirmation to requester?
      // For now, just log and return to avoid re-broadcasting everything.
      // Client's StartCaseCommand itself might handle this by checking context.isCaseStarted()
      // first.
      return;
    }
    if (!started && !this.caseStarted) { // Trying to stop an already stopped case
      logGameMessage("setCaseStarted(false) called, but case was already not started.");
      return;
    }

    this.caseStarted = started; // Set the context's flag

    if (started) {
      logGameMessage(
              "Case '"
                      + (selectedCase != null ? selectedCase.getTitle() : "Unknown")
                      + "' is being started.");

      // *** NOTIFY GameSession TO UPDATE ITS STATE ***
      if (this.gameSession != null) {
        this.gameSession.setSessionState(GameSessionState.ACTIVE); // New method in GameSession
      } else {
        logGameMessage(
                "CRITICAL ERROR: gameSession is null in GameContextServer. Cannot update session state.");
        // This would be a major issue.
      }
      // *** END NOTIFICATION ***

      broadcastInitialCaseDetails(); // Now broadcast all the initial game data
    } else {
      logGameMessage(
              "Case '"
                      + (selectedCase != null ? selectedCase.getTitle() : "Unknown")
                      + "' has been stopped/reset (caseStarted=false).");
    }
  }

  private void broadcastInitialCaseDetails() {
    if (selectedCase == null) {
      logGameMessage("Error: Cannot broadcast initial case details, selectedCase is null.");
      broadcastToSession(
              new TextMessage("Critical error: Case data missing, cannot start.", true), null);
      return;
    }

    // 1. Broadcast Case Invitation:
    //    This is often sent by GameSession when both players join and are ready for 'start case'
    // prompt.
    //    If 'start case' command is what TRULY starts the game flow, then invitation makes more
    // sense here.
    //    Let's assume for now GameSession handled the invitation prompt BEFORE this command.
    //    If not, uncomment and send it here:
    //    broadcastToSession(new TextMessage("--- Case Invitation ---\n" +
    // selectedCase.getInvitation() +
    //        "\n\nInvestigation commencing...", false), null);

    // 2. Broadcast Case Description
    logGameMessage("Broadcasting case description...");
    broadcastToSession(
            new TextMessage("--- Case Description ---\n" + selectedCase.getDescription(), false), null);

    // 3. Broadcast Tasks
    logGameMessage("Broadcasting tasks...");
    if (taskList != null && !taskList.getTasks().isEmpty()) {
      StringBuilder taskMessage = new StringBuilder("--- Case Tasks ---\n");
      List<String> tasks = taskList.getTasks();
      for (int i = 0; i < tasks.size(); i++) {
        taskMessage.append((i + 1)).append(". ").append(tasks.get(i)).append("\n");
      }
      broadcastToSession(new TextMessage(taskMessage.toString().trim(), false), null);
    } else {
      broadcastToSession(new TextMessage("No tasks available for this case.", false), null);
    }

    logger.info("[SESS_CTX:{}] Broadcasting ranking criteria...", gameSession.getSessionId());
    List<JsonDTO.CaseFile.RankTierData> tiers = selectedCase.getRankingTiers();
    if (tiers != null && !tiers.isEmpty()) {
      StringBuilder rankMessage = new StringBuilder("--- Rank Evaluation ---\n");
      rankMessage.append("Your final rank will be determined by the number of 'deduce' commands used:\n");

      tiers.sort(Comparator.comparingInt(JsonDTO.CaseFile.RankTierData::getMaxDeductions));

      // Use a final, single-element array as a mutable container
      final int[] lastMax = { -1 }; // Start at -1 to handle a range starting at 0

      for (JsonDTO.CaseFile.RankTierData tier : tiers) {
        if (tier.isDefaultRank()) continue;

        int lowerBound = lastMax[0] + 1;
        int upperBound = tier.getMaxDeductions();
        String range;

        if (lowerBound > upperBound) continue;
        if (lowerBound == upperBound) {
          range = String.valueOf(lowerBound);
        } else {
          range = lowerBound + "-" + upperBound;
        }

        rankMessage.append(String.format("  - %-20s: %s deductions\n", tier.getRankName(), range));
        lastMax[0] = upperBound; // Modify the content of the array
      }

      // This lambda can now safely access the final 'lastMax' array reference
      tiers.stream().filter(JsonDTO.CaseFile.RankTierData::isDefaultRank).findFirst().ifPresent(tier -> {
        rankMessage.append(String.format("  - %-20s: %d+ deductions\n", tier.getRankName(), lastMax[0] + 1));
      });

      broadcastToSession(new TextMessage(rankMessage.toString().trim(), false), null);
    }

    // 4. Broadcast Starting Room Details
    // Both players start in the same room defined by the case.
    // Get the starting room based on player1 (could be player2, it's the same initial room for the
    // case)
    Room startingRoom;
    if (player1Detective != null
            && player1Detective.getCurrentRoom() != null) { // Check if P1 detective and room are set
      startingRoom = player1Detective.getCurrentRoom();
    } else if (player2Detective != null
            && player2Detective.getCurrentRoom() != null) { // Fallback to P2 if P1 not fully init
      startingRoom = player2Detective.getCurrentRoom();
    } else {
      // Fallback if detective rooms aren't set yet by initializePlayerStartingState
      // (which should have happened before case can be started)
      startingRoom = getRoomByName(selectedCase.getStartingRoom());
      if (startingRoom != null && player1Detective != null)
        player1Detective.setCurrentRoom(startingRoom);
      if (startingRoom != null && player2Detective != null)
        player2Detective.setCurrentRoom(startingRoom);
    }

    if (startingRoom != null) {
      logGameMessage("Broadcasting starting location: " + startingRoom.getName());
      broadcastToSession(
              new TextMessage(
                      "\nYou are now at the starting location: " + startingRoom.getName(), false),
              null);

      // Create one DTO for the room and send it to both.
      // The occupants list will be from the server's perspective of who is in that room.
      List<String> objectNames =
              startingRoom.getObjects().values().stream()
                      .map(GameObject::getName)
                      .collect(Collectors.toList());

      List<String> occupantNamesForBroadcast = new ArrayList<>();
      // Player 1 (if in starting room - should be)
      if (player1Detective != null
              && player1Detective.getCurrentRoom() != null
              && player1Detective.getCurrentRoom().getName().equalsIgnoreCase(startingRoom.getName())) {
        ClientSession p1Session = gameSession.getClientSessionById(player1Id);
        occupantNamesForBroadcast.add(p1Session != null ? p1Session.getDisplayId() : "Player 1");
      }
      // Player 2 (if in starting room - should be)
      if (player2Detective != null
              && player2Detective.getCurrentRoom() != null
              && player2Detective.getCurrentRoom().getName().equalsIgnoreCase(startingRoom.getName())) {
        ClientSession p2Session = gameSession.getClientSessionById(player2Id);
        occupantNamesForBroadcast.add(p2Session != null ? p2Session.getDisplayId() : "Player 2");
      }
      // NPCs in starting room
      for (Suspect s : suspects) {
        if (s.getCurrentRoom() != null
                && s.getCurrentRoom().getName().equalsIgnoreCase(startingRoom.getName())) {
          occupantNamesForBroadcast.add(s.getName());
        }
      }
      if (watson != null
              && watson.getCurrentRoom() != null
              && watson.getCurrentRoom().getName().equalsIgnoreCase(startingRoom.getName())) {
        occupantNamesForBroadcast.add("Dr. Watson");
      }

      Map<String, String> exits =
              startingRoom.getNeighbors().entrySet().stream()
                      .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().getName()));
      RoomDescriptionDTO initialRoomDTO =
              new RoomDescriptionDTO(
                      startingRoom.getName(),
                      startingRoom.getDescription(),
                      objectNames,
                      occupantNamesForBroadcast, // This is a general view, client 'look' might be more
                      // personalized
                      exits);

      broadcastToSession(initialRoomDTO, null);
    } else {
      logGameMessage("Error: Starting location could not be determined for broadcast.");
      broadcastToSession(
              new TextMessage("Error: Starting location not found for the case.", true), null);
    }

    logGameMessage("Broadcasting 'type help' message...");
    broadcastToSession(new TextMessage("\nType 'help' to see available commands.", false), null);
  }

  @Override
  public CaseData getSelectedCase() {
    return selectedCase;
  }

  @Override
  public Detective getPlayerDetective(String playerId) {
    if (playerId == null) return null;
    if (player1Id != null && player1Id.equals(playerId)) return player1Detective;
    if (player2Id != null && player2Id.equals(playerId)) return player2Detective;
    logGameMessage("Warning: getPlayerDetective called for unknown or null playerId: " + playerId);
    return null;
  }

  @Override
  public Room getCurrentRoomForPlayer(String playerId) {
    Detective d = getPlayerDetective(playerId);
    return d != null ? d.getCurrentRoom() : null;
  }

  @Override
  public String getOccupantsDescriptionInRoom(Room room, String askingPlayerId) {
    if (room == null) {
      logGameMessage("Error: getOccupantsDescriptionInRoom called with null room.");
      return "Occupants: Error determining room";
    }
    List<String> occupantNames = new ArrayList<>();
    ClientSession p1Session = gameSession.getClientSessionById(player1Id);
    ClientSession p2Session = gameSession.getClientSessionById(player2Id);

    // Player 1
    if (player1Detective != null
            && player1Detective.getCurrentRoom() != null
            && player1Detective.getCurrentRoom().getName().equalsIgnoreCase(room.getName())) {
      if (player1Id == null || !player1Id.equals(askingPlayerId)) {
        occupantNames.add(p1Session != null ? p1Session.getDisplayId() : "Player 1 (Host)");
      }
    }
    // Player 2
    if (player2Detective != null
            && player2Detective.getCurrentRoom() != null
            && player2Detective.getCurrentRoom().getName().equalsIgnoreCase(room.getName())) {
      if (player2Id == null || !player2Id.equals(askingPlayerId)) {
        occupantNames.add(p2Session != null ? p2Session.getDisplayId() : "Player 2");
      }
    }
    // Suspects
    for (Suspect suspect : suspects) {
      if (suspect.getCurrentRoom() != null
              && suspect.getCurrentRoom().getName().equalsIgnoreCase(room.getName())) {
        occupantNames.add(suspect.getName());
      }
    }
    // Watson
    if (watson != null
            && watson.getCurrentRoom() != null
            && watson.getCurrentRoom().getName().equalsIgnoreCase(room.getName())) {
      occupantNames.add("Dr. Watson");
    }
    return occupantNames.isEmpty()
            ? "Occupants: None"
            : "Occupants: " + String.join(", ", occupantNames);
  }

  @Override
  public TaskList getTaskList() {
    return taskList;
  }

  @Override
  public DoctorWatson getWatson() {
    return watson;
  }

  @Override
  public List<Suspect> getAllSuspects() {
    return Collections.unmodifiableList(suspects);
  }

  @Override
  public boolean movePlayer(String playerId, String direction) {
    Detective movingPlayer = getPlayerDetective(playerId);
    if (movingPlayer == null) {
      logGameMessage("Error: movePlayer called for null detective (playerId: " + playerId + ")");
      sendResponseToPlayer(
              playerId,
              new TextMessage("Error: Player context not found.", true)); // Send error to player
      return false;
    }
    Room oldRoom = movingPlayer.getCurrentRoom();
    if (oldRoom == null) {
      logGameMessage(
              "Error: movePlayer called for detective not in a room (playerId: " + playerId + ")");
      sendResponseToPlayer(
              playerId, new TextMessage("Error: Your current location is unknown. Cannot move.", true));
      return false;
    }

    Room newRoom = oldRoom.getNeighbor(direction.toLowerCase());

    if (newRoom != null) {
      // 1. Move the player
      movingPlayer.setCurrentRoom(newRoom);
      logGameMessage(
              "Player "
                      + playerId
                      + " moved from "
                      + oldRoom.getName()
                      + " to "
                      + newRoom.getName()
                      + " (server state updated).");

      // 2. NPCs take their turn to move (AFTER player has moved)
      // The triggeringPlayerId here is the one whose move initiated this round of NPC updates.
      updateNpcMovements(playerId); // This method will now also broadcast NpcMovedDTOs

      // 3. Notify the OTHER player about the moving player's move
      // This should happen before sending the new room description to the moving player,
      // so the other player knows the context if they receive NpcMovedDTOs next.
      notifyPlayerMove(playerId, newRoom, oldRoom);

      // 4. Construct and send the RoomDescriptionDTO for the moving player's NEW room
      // This will reflect NPC positions AFTER they have moved.
      sendRoomDescriptionToPlayer(playerId, newRoom); // This helper method constructs the DTO

      return true;
    } else {
      // Player could not move in that direction
      sendResponseToPlayer(
              playerId,
              new TextMessage(
                      "You can't move " + direction + " from " + oldRoom.getName() + ".", false));
      return false;
    }
  }

  private void sendRoomDescriptionToPlayer(String playerId, Room room) {
    if (room == null || playerId == null) return;
    // It constructs the DTO based on the CURRENT state of 'room' and its occupants
    List<String> objectNames =
            room.getObjects().values().stream().map(GameObject::getName).collect(Collectors.toList());

    // Get occupants description specific to the 'playerId' view for their new room
    String occupantsStr = getOccupantsDescriptionInRoom(room, playerId);
    List<String> occupantNamesList = new ArrayList<>();
    if (occupantsStr != null
            && !occupantsStr.equalsIgnoreCase("Occupants: None")
            && occupantsStr.startsWith("Occupants: ")) {
      String[] names = occupantsStr.substring("Occupants: ".length()).split(",\\s*");
      for (String name : names) {
        if (!name.trim().isEmpty()) occupantNamesList.add(name.trim());
      }
    }

    Map<String, String> exits =
            room.getNeighbors().entrySet().stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().getName()));
    sendResponseToPlayer(
            playerId,
            new RoomDescriptionDTO(
                    room.getName(), room.getDescription(), objectNames, occupantNamesList, exits));
  }

  @Override
  public void addJournalEntry(JournalEntryDTO entry) {
    if (journal.addEntry(entry)) {
      logGameMessage(
              "Journal entry added by " + entry.getContributorPlayerId() + ": " + entry.getText());
      broadcastToSession(entry, null); // Broadcast the new DTO to all players
      // Send confirmation only to the contributor
      sendResponseToPlayer(
              entry.getContributorPlayerId(),
              new TextMessage("Your note was added to the journal.", false));
    } else {
      sendResponseToPlayer(
              entry.getContributorPlayerId(),
              new TextMessage("Note was a duplicate and not added.", false));
    }
  }

  @Override
  public List<JournalEntryDTO> getJournalEntries(String playerId) {
    return journal.getEntries().stream()
            .sorted(Comparator.comparingLong(JournalEntryDTO::getTimestamp))
            .collect(Collectors.toList());
  }

  @Override
  public void sendResponseToPlayer(String playerId, Serializable responseDto) {
    ClientSession client = gameSession.getClientSessionById(playerId);
    if (client != null) {
      client.send(responseDto);
    } else {
      logGameMessage(
              "Error: Attempted to send DTO to null or disconnected client: "
                      + playerId
                      + ". DTO: "
                      + responseDto.getClass().getSimpleName());
    }
  }

  @Override
  public void broadcastToSession(Serializable dto, String excludePlayerId) {
    gameSession.broadcast(dto, excludePlayerId);
  }

  @Override
  public void notifyPlayerMove(String movingPlayerId, Room newRoom, Room oldRoom) {
    ClientSession otherPlayerSession = gameSession.getOtherPlayer(movingPlayerId);
    ClientSession movingPlayerSession = gameSession.getClientSessionById(movingPlayerId);

    // Ensure all objects are non-null before trying to access their properties
    if (otherPlayerSession != null
            && movingPlayerSession != null
            && oldRoom != null
            && newRoom != null) {
      otherPlayerSession.send(
              new TextMessage(
                      movingPlayerSession.getDisplayId()
                              + " moved from "
                              + oldRoom.getName()
                              + " to "
                              + newRoom.getName()
                              + ".",
                      false));
    }
  }

  // --- Exam Logic ---
  @Override
  public boolean canStartFinalExam(String playerId) {
    // Only player1 (host) can initiate. Case must be started.
    // Removed !examActiveForSession to allow re-starting if needed or to handle retries differently,
    // though typically startExamProcess handles reset.
    // However, if an exam IS active, we probably shouldn't restart it unless it's a specific retry.
    // But for "canStart", as long as they are host and case is started, they *could* initiate,
    // potentially resetting the current one.

    boolean isHost = isPlayerHost(playerId);
    // If exam is active, we might deny a "Start" command but allow "Retry".
    // But for now, we follow the simpler check.
    boolean conditionsMet = isCaseStarted();

    if (!isHost) {
      logGameMessage("Player " + playerId + " (guest) attempted to start exam directly. Denied.");
    }
    if (!conditionsMet) {
      logGameMessage(
              "Exam conditions not met for player "
                      + playerId
                      + " (isCaseStarted: "
                      + isCaseStarted()
                      + ")");
    }
    return isHost && conditionsMet;
  }

  @Override
  public void startExamProcess(String playerId) { // playerId is the initiator
    if (!canStartFinalExam(playerId)) {
      sendResponseToPlayer(
              playerId,
              new TextMessage(
                      "You cannot start the final exam at this time (not host or conditions not met).",
                      true));
      return;
    }
    if (selectedCase.getFinalExam() == null || selectedCase.getFinalExam().getQuestions().isEmpty()) {
      broadcastToSession(
              new TextMessage("Error: No final exam questions configured for this case.", true), null);
      return;
    }

    this.examActiveForSession = true;
    this.finalExam = selectedCase.getFinalExam();
    this.playerAnswers = new HashMap<>(); // Clear answers for a fresh start
    this.currentExamQuestionIndex = 0;

    ClientSession hostSession = gameSession.getClientSessionById(player1Id); // Host is player1Id
    String hostDisplay = hostSession != null ? hostSession.getDisplayId() : "The Host";

    logGameMessage(
            "Interactive final exam initiated by host: "
                    + hostDisplay
                    + ". Sending first question to all.");
    broadcastToSession(
            new TextMessage("--- Final Exam Initiated by " + hostDisplay + " ---", false), null);
      broadcastToSession(new InitiateFinalExamDTO(finalExam), null);
    sendNextExamQuestionToSession();
  }

  @Override
  public void processSubmitQuestionAnswer(String playerId, int questionIndex, Map<String, String> answers) {
      if (!isPlayerHost(playerId)) {
          sendResponseToPlayer(playerId, new TextMessage("Only the host can answer the exam.", true));
          return;
      }
      if (!examActiveForSession || finalExam == null) {
          sendResponseToPlayer(playerId, new TextMessage("No exam is currently active.", true));
          return;
      }
      if (questionIndex != currentExamQuestionIndex) {
          // Ignore answers for wrong question index (sync issue?)
          return;
      }

      // Store answers
      playerAnswers.put(questionIndex, answers);

      // Advance to next question or finish
      if (currentExamQuestionIndex < finalExam.getQuestions().size() - 1) {
          currentExamQuestionIndex++;
          sendNextExamQuestionToSession();
      } else {
          evaluateAndBroadcastExamResults(playerId);
      }
  }

  public boolean isPlayerHost(String playerId) {
    return this.player1Id != null && this.player1Id.equals(playerId);
  }

  // Inside server.GameContextServer.java
  @Override
  public void processRequestStartCase(String requestingPlayerId) {
    logGameMessage(
            "PROCESS_REQUEST_START_CASE: by PlayerId="
                    + requestingPlayerId
                    + ", CaseStarted="
                    + isCaseStarted()
                    + ", IsHost="
                    + isPlayerHost(requestingPlayerId));

    if (isCaseStarted()) {
      sendResponseToPlayer(
              requestingPlayerId, new TextMessage("The case has already started.", false));
      return;
    }
    if (isPlayerHost(requestingPlayerId)) { // If host typed "request start case"
      sendResponseToPlayer(
              requestingPlayerId,
              new TextMessage("As host, you can directly use the 'start case' command.", false));
      return;
    }

    // If it's a guest making the request
    if (player1Id != null) { // Check if host (player1Id) is actually connected/present
      ClientSession requestingPlayerSession = gameSession.getClientSessionById(requestingPlayerId);
      String requesterDisplay =
              (requestingPlayerSession != null)
                      ? requestingPlayerSession.getDisplayId()
                      : "Your partner (" + requestingPlayerId.substring(0, 4) + "..)";

      // Send prompt to HOST (player1Id)
      logGameMessage(
              "PROCESS_REQUEST_START_CASE: Sending prompt to host "
                      + player1Id
                      + " about request from "
                      + requestingPlayerId);
      sendResponseToPlayer(
              player1Id,
              new TextMessage(
                      requesterDisplay + " has requested to start the case. Type 'start case' to begin.",
                      false));

      // Send confirmation to GUEST (requestingPlayerId)
      sendResponseToPlayer(
              requestingPlayerId,
              new TextMessage("Request sent to the host to start the case.", false));
    } else {
      logGameMessage(
              "PROCESS_REQUEST_START_CASE: Host (player1Id) is null or not available. Cannot process request from "
                      + requestingPlayerId);
      sendResponseToPlayer(
              requestingPlayerId,
              new TextMessage("The host is not currently available to start the case.", true));
    }
  }

  @Override
  public void processRequestInitiateExam(String requestingPlayerId) {
    if (!isCaseStarted()) {
      sendResponseToPlayer(
              requestingPlayerId,
              new TextMessage("The case has not started yet. Cannot request exam.", true));
      return;
    }
    if (examActiveForSession) {
      sendResponseToPlayer(
              requestingPlayerId, new TextMessage("An exam is already in progress.", false));
      return;
    }
    if (isPlayerHost(requestingPlayerId)) {
      sendResponseToPlayer(
              requestingPlayerId,
              new TextMessage("As host, you can directly use 'final exam' to initiate.", false));
      return;
    }
    // Guest is requesting
    if (player1Id != null) { // If host is present
      ClientSession requestingPlayerSession = gameSession.getClientSessionById(requestingPlayerId);
      String requesterDisplay =
              requestingPlayerSession != null ? requestingPlayerSession.getDisplayId() : "Your partner";

      sendResponseToPlayer(
              player1Id,
              new FinalExamRequestDTO(requestingPlayerId, requesterDisplay));
      sendResponseToPlayer(
              requestingPlayerId,
              new TextMessage("Request sent to host to initiate the final exam.", false));
      logGameMessage(
              "Player "
                      + requestingPlayerId
                      + " requested final exam. Host "
                      + player1Id
                      + " notified.");
    } else {
      sendResponseToPlayer(
              requestingPlayerId, new TextMessage("Host is not available to start the exam.", true));
    }
  }

  private void sendNextExamQuestionToSession() {
    logGameMessage(
            "SEND_NEXT_Q_TO_SESS: examActive="
                    + examActiveForSession
                    + ", CurrentIndex="
                    + currentExamQuestionIndex
                    + ", QuestionsListSize="
                    + (finalExam != null ? finalExam.getQuestions().size() : "NULL_LIST"));

    if (!examActiveForSession) {
      logGameMessage("SEND_NEXT_Q_TO_SESS: Exam not active, exiting send logic.");
      return;
    }

    if (finalExam == null || finalExam.getQuestions().isEmpty()) {
      logGameMessage(
              "SEND_NEXT_Q_TO_SESS: CRITICAL - finalExam is null or empty! Cannot proceed with exam.");
      // Attempt to end exam gracefully if possible, or send error to host
      if (player1Id != null) {
        sendResponseToPlayer(
                player1Id,
                new TextMessage("Error: Exam questions are missing. Cannot continue exam.", true));
      }
      evaluateAndBroadcastExamResults(player1Id); // This will likely show 0/0 or error
      return;
    }

    if (currentExamQuestionIndex < finalExam.getQuestions().size()) {
      // Send next question
        FinalExamQuestionDTO q = finalExam.getQuestions().get(currentExamQuestionIndex);
        Map<String, String> selectedAnswers = playerAnswers.getOrDefault(currentExamQuestionIndex, new HashMap<>());
        ExamQuestionDTO questionDTO =
                new ExamQuestionDTO(currentExamQuestionIndex, finalExam.getQuestions().size(), q.getQuestionPrompt(), q.getSlots(), selectedAnswers);

      logGameMessage(
              "SEND_NEXT_Q_TO_SESS: Broadcasting Q_DTO for Q"
                      + (currentExamQuestionIndex + 1)
                      + ": \""
                      + q.getQuestionPrompt().substring(0, Math.min(20, q.getQuestionPrompt().length()))
                      + "...\"");
      broadcastToSession(questionDTO, null);

      // Send specific prompt to host
      if (player1Id != null) {
        logGameMessage(
                "SEND_NEXT_Q_TO_SESS: Sending prompt to host "
                        + player1Id
                        + " for Q"
                        + (currentExamQuestionIndex + 1));
        sendResponseToPlayer(
                player1Id,
                new TextMessage(
                        "Host, please submit your answer for Q" + (currentExamQuestionIndex + 1) + ".",
                        false));
      }
      // Notify guest
      if (player2Id != null
              && (!player2Id.equals(player1Id))) { // Ensure guest is not also host (single player test)
        ClientSession hostSess = gameSession.getClientSessionById(player1Id);
        String hostDisp =
                (hostSess != null)
                        ? hostSess.getDisplayId()
                        : (player1Detective != null ? player1Detective.getPlayerId() : "The Host");
        logGameMessage(
                "SEND_NEXT_Q_TO_SESS: Notifying guest "
                        + player2Id
                        + " that host is answering Q"
                        + (currentExamQuestionIndex + 1));
        sendResponseToPlayer(
                player2Id,
                new TextMessage(
                        hostDisp
                                + " is answering exam question "
                                + (currentExamQuestionIndex + 1)
                                + "/"
                                + finalExam.getQuestions().size()
                                + "...",
                        false));
      }
    } else {
        sendResponseToPlayer(player1Id, new TextMessage("You have answered all questions. Type 'submit exam' to finalize your answers.", false));
    }
  }



  private void evaluateAndBroadcastExamResults(String hostPlayerId) {
    logger.info("[SESS_CTX:{}] EVAL_EXAM_RESULTS: Starting evaluation for host {}", gameSession.getSessionId(), hostPlayerId);

    if (finalExam == null) {
      logger.error("[SESS_CTX:{}] EVAL_EXAM_RESULTS: Error - finalExam is null. Cannot evaluate.", gameSession.getSessionId());
      if (player1Id != null && player1Id.equals(hostPlayerId)) {
        sendResponseToPlayer(player1Id, new TextMessage("Error during exam evaluation (missing questions).", true));
      }
      resetServerExamState();
      return;
    }

    if (playerAnswers == null) {
      logger.warn("[SESS_CTX:{}] EVAL_EXAM_RESULTS: Warning - playerAnswers is null. Assuming 0 score.", gameSession.getSessionId());
      playerAnswers = new HashMap<>();
    }

    int score = 0;
    List<String> reviewableAnswersDetails = new ArrayList<>();
    int totalQuestions = finalExam.getQuestions().size();

    for (int i = 0; i < totalQuestions; i++) {
        FinalExamQuestionDTO actualQuestion = finalExam.getQuestions().get(i);
        Map<String, String> correctCombination = actualQuestion.getCorrectCombination();
        Map<String, String> hostAnswer = playerAnswers.get(i);

        if (hostAnswer != null && hostAnswer.equals(correctCombination)) {
            score++;
        } else {
            String reviewDetail = String.format("Q%d: %s\n   Your Answer: '%s'", (i + 1), actualQuestion.getQuestionPrompt(), (hostAnswer != null ? hostAnswer : "Not answered"));
            reviewableAnswersDetails.add(reviewDetail);
        }
    }

    logger.info("[SESS_CTX:{}] EVAL_EXAM_RESULTS: Host {} score: {}/{}. Total team deductions: {}", gameSession.getSessionId(), hostPlayerId, score, totalQuestions, this.sessionDeduceCount);
    // --- NEW DYNAMIC RANK EVALUATION ---
    Rank finalRank = RankEvaluator.evaluate(this.sessionDeduceCount, this.selectedCase);
    String finalRankString = (finalRank != null) ? finalRank.getRankName() : "Unranked";

    Detective hostDetective = getPlayerDetective(hostPlayerId);
    if (hostDetective != null) {
      hostDetective.setFinalExamScore(score);
      hostDetective.setRank(finalRank);
    }

    Detective guestDetective = getPlayerDetective(player2Id);
    if (guestDetective != null) {
      guestDetective.setFinalExamScore(score);
      guestDetective.setRank(finalRank);
    }
    // --- END DYNAMIC RANK EVALUATION ---

    // Determine feedback message based on score
    String feedback;
    ClientSession hostSession = gameSession.getClientSessionById(hostPlayerId);
    String hostDisplay = (hostSession != null) ? hostSession.getDisplayId() : "The Host";

    if (score == totalQuestions) {
      feedback = "Outstanding! The case is solved perfectly by " + hostDisplay + "!";
    } else if (score >= totalQuestions * 0.5) {
      feedback = "Good effort by " + hostDisplay + "! Key aspects uncovered.";
    } else {
      feedback = "The mystery remains largely unsolved by " + hostDisplay + ". Further investigation was needed.";
    }

    boolean caseSolved = (score == totalQuestions);
    String winningMessage = (caseSolved && this.selectedCase != null) ? this.selectedCase.getWinningMessage() : null;

    // Create the final result DTO
    ExamResultDTO resultDTO = new ExamResultDTO(
            score,
            totalQuestions,
            feedback,
            finalRankString,
            reviewableAnswersDetails,
            winningMessage,
            caseSolved
    );

    logger.info("[SESS_CTX:{}] EVAL_EXAM_RESULTS: Broadcasting ExamResultDTO: Score={}, Rank={}", gameSession.getSessionId(), score, finalRankString);

    broadcastToSession(resultDTO, null);
    broadcastToSession(new TextMessage("--- Final Exam Concluded ---", false), null);
    broadcastToSession(new TextMessage("1. Continue Playing", false), null);
    if (caseSolved) {
        broadcastToSession(new TextMessage("2. Return to Main Menu", false), null);
    }

    // Instead of full reset, we just mark it as inactive but keep the data (answers) for potential Retry.
    this.examActiveForSession = false;
    // We do NOT nullify finalExam or playerAnswers here.
    logGameMessage("Final exam concluded for session " + gameSession.getSessionId());
  }

  // Helper to reset server-side exam state variables (Only call on new case load or hard reset)
  private void resetServerExamState() {
    this.examActiveForSession = false;
    this.finalExam = null;
    this.playerAnswers = null;
    this.currentExamQuestionIndex = 0;
    logGameMessage(
            "Server-side exam state has been reset for session " + gameSession.getSessionId());
  }

  @Override
  public void processUpdateDisplayName(String playerId, String newDisplayName) {
    ClientSession client = gameSession.getClientSessionById(playerId);
    if (client != null) {
      String oldDisplayName = client.getDisplayId();
      if (newDisplayName != null
              && !newDisplayName.equals(oldDisplayName)
              && !newDisplayName.trim().isEmpty()
              && newDisplayName.length() < 25) {
        client.setDisplayId(newDisplayName); // Update on the ClientSession object
        logGameMessage(
                "Player "
                        + playerId
                        + " (formerly "
                        + oldDisplayName
                        + ") changed display name to "
                        + newDisplayName);

        // Broadcast the change to all players in the session
        PlayerNameChangedDTO pncDTO =
                new PlayerNameChangedDTO(playerId, oldDisplayName, newDisplayName);
        broadcastToSession(
                pncDTO, null); // Send to all, including the changer for confirmation sync

        // If this player is hosting a public game that's still in the lobby list,
        // the GameSessionManager needs to be notified to update the public game info.
        gameSession.notifyNameChangeToManagerIfHost(playerId, newDisplayName);

      } else {
        assert newDisplayName != null;
        if (newDisplayName.equals(oldDisplayName)) {
          // Name is the same, just confirm back to sender if needed (optional)
          sendResponseToPlayer(
                  playerId,
                  new TextMessage("Your display name is already " + newDisplayName + ".", false));
        } else {
          sendResponseToPlayer(
                  playerId, new TextMessage("New display name is invalid or too long.", true));
        }
      }
    } else {
      logGameMessage("Error: processUpdateDisplayName received for unknown playerId: " + playerId);
    }
  }

  @Override
  public WatsonHintResponseDTO askWatsonForHint(String playerId) {
    Detective player = getPlayerDetective(playerId);
    if (player == null) {
      logGameMessage("askWatsonForHint: Player " + playerId + " not found in context.");
      return new WatsonHintResponseDTO("Error: Player context not found.", false);
    }
    if (this.watson == null) {
      return new WatsonHintResponseDTO("Dr. Watson is not available in this case.", false);
    }
    Room playerRoom = player.getCurrentRoom();
    Room watsonRoom = this.watson.getCurrentRoom();
    if (playerRoom == null) {
      return new WatsonHintResponseDTO("Your location is unknown. Cannot determine if Watson is present.", false);
    }
    if (watsonRoom == null) {
      return new WatsonHintResponseDTO("Dr. Watson's location is currently unknown.", false);
    }
    if (watsonRoom.getName().equalsIgnoreCase(playerRoom.getName())) {
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



  @Override
  public void updateNpcMovements(String triggeringPlayerId) {
    if (!isCaseStarted()) {
      logGameMessage("NPC Movement SKIPPED (MP): Case not started.");
      return;
    }

    // --- NEW: Determine all currently occupied rooms ---
    Set<String> occupiedRoomNames = new HashSet<>();
    if (player1Detective != null && player1Detective.getCurrentRoom() != null) {
      occupiedRoomNames.add(player1Detective.getCurrentRoom().getName().toLowerCase());
    }
    if (player2Detective != null && player2Detective.getCurrentRoom() != null) {
      occupiedRoomNames.add(player2Detective.getCurrentRoom().getName().toLowerCase());
    }

    logGameMessage("NPC Movement START. Occupied rooms: " + occupiedRoomNames);

    // --- Move Suspects ---
    for (Suspect suspect : this.suspects) {
      Room oldSuspectRoom = suspect.getCurrentRoom();

      if (oldSuspectRoom == null) {
        logGameMessage("Suspect " + suspect.getName() + " is not in any room, cannot move.");
        continue;
      }

      // NEW RULE: If a player is in the same room as the suspect, the suspect does not move.
      if (occupiedRoomNames.contains(oldSuspectRoom.getName().toLowerCase())) {
        logGameMessage("Suspect " + suspect.getName() + " stays in " + oldSuspectRoom.getName() + " (room is occupied by a player).");
        continue;
      }

      Map<String, Room> neighbors = oldSuspectRoom.getNeighbors();
      if (neighbors.isEmpty()) {
        logGameMessage("Suspect " + suspect.getName() + " in " + oldSuspectRoom.getName() + " has no neighbors, stays put.");
        continue;
      }

      // NEW RULE: Filter out neighbors that are occupied by players.
      List<Room> possibleMoves = neighbors.values().stream()
              .filter(room -> !occupiedRoomNames.contains(room.getName().toLowerCase()))
              .collect(Collectors.toList());

      if (!possibleMoves.isEmpty()) {
        Room newSuspectRoom = possibleMoves.get(random.nextInt(possibleMoves.size()));
        suspect.setCurrentRoom(newSuspectRoom);
        logGameMessage("Suspect " + suspect.getName() + " moved from " + oldSuspectRoom.getName() + " to " + newSuspectRoom.getName());
      } else {
        logGameMessage("Suspect " + suspect.getName() + " has no unoccupied neighbors, stays put in " + oldSuspectRoom.getName());
      }
    }

    // --- Move Watson (applying the same logic) ---
    if (this.watson != null) {
      Room oldWatsonRoom = this.watson.getCurrentRoom();

      if (oldWatsonRoom != null) {
        // NEW RULE: If a player is in the same room, Watson does not move.
        if (occupiedRoomNames.contains(oldWatsonRoom.getName().toLowerCase())) {
          logGameMessage("Dr. Watson stays in " + oldWatsonRoom.getName() + " (room is occupied by a player).");
        } else {
          Map<String, Room> watsonNeighbors = oldWatsonRoom.getNeighbors();
          if (!watsonNeighbors.isEmpty()) {
            // NEW RULE: Filter out occupied neighbors.
            List<Room> possibleMoves = watsonNeighbors.values().stream()
                    .filter(room -> !occupiedRoomNames.contains(room.getName().toLowerCase()))
                    .collect(Collectors.toList());

            if (!possibleMoves.isEmpty()) {
              Room newWatsonRoom = possibleMoves.get(random.nextInt(possibleMoves.size()));
              this.watson.setCurrentRoom(newWatsonRoom);
              logGameMessage("Dr. Watson moved from " + oldWatsonRoom.getName() + " to " + newWatsonRoom.getName());
            } else {
              logGameMessage("Dr. Watson has no unoccupied neighbors, stays put in " + oldWatsonRoom.getName());
            }
          }
        }
      }
    }

    logGameMessage("NPC Movement END.");
  }


  @Override
  public void handlePlayerExitRequest(String playerId) {
    logger.info("[SESS_CTX:{}] Player {} has requested to exit the game session.", gameSession.getSessionId(), playerId);
    // Delegate the actual session termination and notification to GameSession
    gameSession.playerRequestsExit(playerId);
  }

  @Override
  public void handlePlayerCancelLobby(String playerId) {
    logger.info("[SESS_CTX:{}] Player {} is cancelling lobby participation.", gameSession.getSessionId(), playerId);
    gameSession.playerCancelsLobby(playerId);
  }

  public void executeCommand(Command command) { // This is the method called by GameSession
    if (command == null) {
      /* ... */
      return;
    }
    if (command.getPlayerId() == null) {
      /* ... */
      return;
    }

    logGameMessage(
            "Context executing command: "
                    + command.getClass().getSimpleName()
                    + " for player "
                    + command.getPlayerId());

    // --- HOST CHECKS ---
    if (command instanceof StartCaseCommand) {
      if (!isPlayerHost(command.getPlayerId())) {
        sendResponseToPlayer(
                command.getPlayerId(),
                new TextMessage(
                        "Only the host can directly start the case. Guests can use 'request start case'.",
                        true));
        return;
      }
    } else if (command instanceof InitiateFinalExamCommand) {
      if (!isPlayerHost(command.getPlayerId())) {
        processRequestInitiateExam(command.getPlayerId());
        return;
      }
    }
    // --- END HOST CHECKS ---

    command.execute(this);
  }

  @Override
  public void processUpdateTaskState(String playerId, int taskIndex, boolean isCompleted) {
    try {
      if (taskList == null) {
        throw new IllegalStateException("Task list has not been initialized for the current case.");
      }
      if (taskIndex >= 0 && taskIndex < taskList.getTasks().size()) {
        taskStates.put(taskIndex, isCompleted);
        logGameMessage(
                "Player "
                        + playerId
                        + " updated task "
                        + taskIndex
                        + " to state: "
                        + (isCompleted ? "Completed" : "Incomplete"));

        TaskStateUpdateDTO updateDTO = new TaskStateUpdateDTO(taskIndex, isCompleted);
        broadcastToSession(updateDTO, null);
      } else {
        throw new IllegalArgumentException("Invalid task index provided: " + taskIndex);
      }
    } catch (Exception e) {
      logGameMessage(
              "Error processing task state update for player "
                      + playerId
                      + " (taskIndex: "
                      + taskIndex
                      + "): "
                      + e.getMessage());

      sendResponseToPlayer(
              playerId,
              new TextMessage("Error updating task: " + e.getMessage(), true));
    }
  }


  @Override
  public int getSessionDeduceCount() {
    return this.sessionDeduceCount;
  }

  @Override
  public void incrementSessionDeduceCount() {
    this.sessionDeduceCount++;
  }

  @Override
  public void processContinueGame(String playerId) {
      // In Multiplayer, continue means sending the current room description to all players
      // so their UI updates and exits the exam view.

      // Get the room of the player initiating the continue (usually host)
      Detective detective = getPlayerDetective(playerId);
      if (detective == null || detective.getCurrentRoom() == null) {
          logGameMessage("Error processing continue game: detective or room not found for player " + playerId);
          return;
      }

      Room currentRoom = detective.getCurrentRoom();
      logGameMessage("Processing Continue Game. Broadcasting room '" + currentRoom.getName() + "' to all players.");

      // Send room description to host
      if (player1Id != null) {
          sendRoomDescriptionToPlayer(player1Id, currentRoom);
      }

      // Send room description to guest
      if (player2Id != null) {
          sendRoomDescriptionToPlayer(player2Id, currentRoom);
      }
  }
}
