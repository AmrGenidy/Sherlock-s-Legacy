package ui;

import client.GameClient;
import client.GameClientStateListener;
import common.NetworkConstants;
import common.dto.PublicGameInfoDTO;
import common.dto.RoomDescriptionDTO;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.stream.Collectors;
import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.application.HostServices;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.control.Alert;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import common.commands.UpdateTaskStateCommand;
import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;
import server.ServerMain;
import singleplayer.SinglePlayerMain;
import ui.util.GameOutputParser;
import ui.util.RoomView;
import ui.util.TextAreaOutputStream;
import client.discovery.DiscoveredGame;
import client.discovery.UdpLanGameDiscoveryService;
import ui.windows.ChatWindow;
import ui.windows.JournalWindow;
import client.exam.FinalExamController;
import client.exam.FinalExamListener;
import common.dto.ExamQuestionDTO;
import common.dto.ExamResultDTO;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import ui.windows.TasksWindow;
import ui.windows.HelpWindow;
import ui.pinboard.PinboardController;

public class MainController implements GameClientStateListener, FinalExamListener {

    private enum UIState {
        MENU,
        FINAL_EXAM,
        CHOOSING_CASE,
        CHOOSING_LANGUAGE,
        CASE_INVITATION,
        GAME_SINGLE,
        GAME_MULTI,
        MULTIPLAYER_MENU,
        ADDING_CASE_TERMINAL,
        JOIN_MENU_TERMINAL,
        LISTING_PUBLIC_GAMES_TERMINAL,
        PROMPT_JOIN_CODE_TERMINAL,
        JOIN_GAME_MENU
    }

    private enum UIMultiplayerSubState {
        NONE,
        CONNECTING,
        MAIN_MENU,
        HOST_OPTIONS,
        CASE_SELECTION,
        LANGUAGE_SELECTION,
        HOSTING_LOBBY,
        JOIN_OPTIONS,
        PUBLIC_GAMES_LIST,
        PRIVATE_GAME_ENTRY,
        IN_LOBBY,
        IN_GAME,
        DISCONNECTED
    }

    @FXML
    private BorderPane mainBorderPane;
    @FXML
    private Button tasksButton;
    @FXML
    private Button journalButton;
    @FXML
    private Button pinboardButton;
    @FXML
    private Button chatButton;
    @FXML
    private Button finalExamButton;
    @FXML
    private Button helpButton;
    @FXML
    private Button exitButton;
    @FXML
    private Label unreadChatLabel;
    @FXML
    private StackPane roomPane;
    @FXML
    private VBox rightInfoPanel;
    @FXML
    private VBox neighboringRoomsContainer;
    @FXML
    private TextArea terminalTextArea;
    @FXML
    private TextField terminalInputField;
    @FXML
    private Label statusLabel;
    @FXML
    private SplitPane bottomSplitPane;

    private GameClient gameClient;
    private Thread gameClientThread;
    private JournalWindow journalWindow;
    private ChatWindow chatWindow;
    private TasksWindow tasksWindow;
    private HelpWindow helpWindow;
    private PinboardController pinboardController;
    private RoomView roomView;
    private int unreadChatCount = 0;

    private VBox mainMenuVBox;
    private List<String> launchArgs;
    private HostServices hostServices;
    private UIState currentState = UIState.MENU;

    private TextAreaOutputStream taos;
    private SinglePlayerMain singlePlayerGame;
    private Thread singlePlayerGameThread;
    private JsonDTO.CaseFile selectedCaseFile; // Temporarily store the case for language selection
    private UIMultiplayerSubState currentMultiplayerSubState = UIMultiplayerSubState.NONE;
    private boolean isSinglePlayer;
    private boolean isHostPlayer;
    private java.util.Map<String, Boolean> taskStates;
    private FinalExamController finalExamController;
    private FinalExamViewController finalExamViewController;
    private Parent finalExamView;
    private ExamResultDTO lastExamResult;
    private volatile boolean manualDisconnect = false;

    // --- Embedded Server & Multiplayer Config ---
    private server.GameServer embeddedServer;
    private Thread embeddedServerThread;
    private boolean embeddedServerRunning = false;
    private String configuredServerHost;
    private int configuredServerPort;
    private client.discovery.LanGameDiscoveryService discoveryService;
    private VBox joinGameMenu;
    private ListView<DiscoveredGame> publicGamesListView;

    @FXML
    public void initialize() {
        this.taskStates = new java.util.HashMap<>();
        terminalTextArea.setEditable(false);
        terminalTextArea.setWrapText(true);

        // Auto-scrolling is now handled by the TextAreaOutputStream

        terminalInputField.setOnAction(event -> handleTerminalInput());
        tasksButton.setOnAction(event -> {
            playSound("click.wav");
            openTasksWindow();
        });
        journalButton.setOnAction(event -> {
            playSound("pageflip.mp3");
            openJournalWindow();
        });

        // Pinboard Button Initialization
        pinboardButton.setOnAction(event -> {
            playSound("click.wav");
            openPinboardWindow();
        });

        chatButton.setOnAction(event -> {
            playSound("click.wav");
            openChatWindow();
        });

        finalExamButton.setOnAction(event -> {
            playSound("click.wav");
            sendCommand("final exam");
        });
        // Ensure final exam button is always enabled
        finalExamButton.setDisable(false);

        helpButton.setOnAction(event -> {
            playSound("click.wav");
            openHelpWindow();
        });

        exitButton.setOnAction(event -> {
            playSound("click.wav");
            sendCommand("exit");
        });

        updateStatus("GUI Ready. Please select a game mode.");
        unreadChatLabel.setVisible(false);
        bottomSplitPane.setDividerPositions(0.7);

        roomView = new RoomView(this);
        roomPane.getChildren().add(roomView);

        // Redirect System.out and System.in
        this.taos = new TextAreaOutputStream(terminalTextArea);
        GameOutputParser parser = new GameOutputParser(this);
        taos.setParser(parser);
        System.setOut(new PrintStream(taos, true));

        this.discoveryService = new client.discovery.UdpLanGameDiscoveryService();

        createMainMenu();
        createJoinGameMenu();
        setupButtonIcons();
        loadFinalExamView();
        updateUIVisibility();
    }

    private void loadFinalExamView() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/FinalExamView.fxml"));
            finalExamView = loader.load();
            finalExamViewController = loader.getController();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void createJoinGameMenu() {
        joinGameMenu = new VBox(15);
        joinGameMenu.setAlignment(Pos.CENTER);
        joinGameMenu.getStyleClass().add("main-menu-container");

        Label title = new Label("Join Multiplayer Game");
        title.getStyleClass().add("title");

        publicGamesListView = new ListView<>();
        publicGamesListView.setPlaceholder(new Label("No public games found on the network."));

        Button joinSelectedButton = new Button("Join Selected Game");
        joinSelectedButton.setOnAction(e -> {
            DiscoveredGame selected = publicGamesListView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                joinGameByDiscovery(selected);
            }
        });

        TextField codeField = new TextField();
        codeField.setPromptText("Enter Join Code");
        Button joinByCodeButton = new Button("Join by Code");
        joinByCodeButton.setOnAction(e -> joinGameByCode(codeField.getText()));

        Button refreshButton = new Button("Refresh");
        refreshButton.setOnAction(e -> refreshPublicGamesList());

        Button backButton = new Button("Back to Main Menu");
        backButton.setOnAction(e -> returnToMainMenu());

        HBox codeBox = new HBox(5, codeField, joinByCodeButton);
        codeBox.setAlignment(Pos.CENTER);

        HBox buttonBox = new HBox(10, joinSelectedButton, refreshButton, backButton);
        buttonBox.setAlignment(Pos.CENTER);

        joinGameMenu.getChildren().addAll(title, publicGamesListView, codeBox, buttonBox);
    }

    private void refreshPublicGamesList() {
        if (publicGamesListView == null) return;

        publicGamesListView.setPlaceholder(new Label("Searching for games..."));
        publicGamesListView.getItems().clear();
        discoveryService.refreshAsync();

        PauseTransition pause = new PauseTransition(Duration.millis(1200));
        pause.setOnFinished(event -> {
            List<DiscoveredGame> publicGames = discoveryService.getCurrentGames().stream()
                    .filter(DiscoveredGame::isPublicGame)
                    .collect(Collectors.toList());
            publicGamesListView.setItems(FXCollections.observableArrayList(publicGames));
            publicGamesListView.setPlaceholder(new Label("No public games found on the network."));
        });
        pause.play();
    }

    private void showCaseInvitation(String invitationText, boolean isHost) {
        VBox invitationBox = new VBox(20);
        invitationBox.setAlignment(Pos.CENTER);
        invitationBox.setStyle("-fx-background-color: #1a1a1a;");

        Label titleLabel = new Label("Case Invitation");
        titleLabel.setStyle("-fx-font-size: 24; -fx-text-fill: #d4af37;");

        TextArea invitationTextArea = new TextArea(invitationText);
        invitationTextArea.setEditable(false);
        invitationTextArea.setWrapText(true);
        invitationTextArea.setStyle(
                "-fx-control-inner-background: #2b2b2b; " +
                        "-fx-text-fill: #e0e0e0; " +
                        "-fx-font-family: 'Georgia'; " +
                        "-fx-font-size: 14px; " +
                        "-fx-border-color: #d4af37; " +
                        "-fx-border-width: 1;"
        );
        invitationTextArea.setPrefWidth(600);
        invitationTextArea.setPrefHeight(400);

        Button startButton = new Button("Start Case");
        startButton.setOnAction(event -> handleStartCase());

        invitationBox.getChildren().addAll(titleLabel, invitationTextArea, startButton);

        if (!isHost) {
            Button cancelButton = new Button("Cancel");
            cancelButton.setOnAction(event -> sendCommand("cancel"));
            invitationBox.getChildren().add(cancelButton);
        }

        Platform.runLater(() -> {
            roomPane.getChildren().clear();
            roomPane.getChildren().add(invitationBox);
            currentState = UIState.CASE_INVITATION;
            updateUIVisibility();
        });
    }

    private void handleStartCase() {
        playSound("click.wav");
        if (isSinglePlayer) {
            // Run in a background thread to avoid freezing the UI
            new Thread(() -> {
                singlePlayerGame.processCommand("start case");
                Platform.runLater(() -> {
                    currentState = UIState.GAME_SINGLE;
                    updateUIVisibility();
                });
            }).start();
        } else {
            sendCommand("start case");
            // The UI will be updated by the server's response
        }
    }

    public void setLaunchArgs(List<String> args) {
        this.launchArgs = args;
    }

    public void setHostServices(HostServices hostServices) {
        this.hostServices = hostServices;
    }

    private void setupButtonIcons() {
        setButtonIcon(tasksButton, "/icons/tasks.png");
        setButtonIcon(journalButton, "/icons/journal.png");
        setButtonIcon(pinboardButton, "/icons/journal.png"); // Reuse journal icon or use pinboard specific
        setButtonIcon(chatButton, "/icons/chat.png");
    }

    private void setButtonIcon(Button button, String iconPath) {
        try {
            Image icon = new Image(getClass().getResourceAsStream(iconPath));
            ImageView iconView = new ImageView(icon);
            iconView.setFitHeight(20);
            iconView.setFitWidth(20);
            button.setGraphic(iconView);
        } catch (Exception e) {
            System.err.println("Could not load icon: " + iconPath);
        }
    }

    private void playSound(String soundFile) {
        try {
            String soundPath = getClass().getResource("/sounds/" + soundFile).toExternalForm();
            Media sound = new Media(soundPath);
            MediaPlayer mediaPlayer = new MediaPlayer(sound);
            mediaPlayer.play();
        } catch (Exception e) {
            System.err.println("Could not play sound: " + soundFile);
        }
    }

    private void createMainMenu() {
        mainMenuVBox = new VBox(15);
        mainMenuVBox.setAlignment(Pos.CENTER);
        mainMenuVBox.getStyleClass().add("main-menu-container");

        Button singlePlayerButton = new Button("Single Player");
        singlePlayerButton.getStyleClass().add("main-menu-button");
        singlePlayerButton.setOnAction(event -> {
            playSound("click.wav");
            handleMainMenuInput("1");
        });

        Button hostMultiplayerButton = new Button("Host Multiplayer Game");
        hostMultiplayerButton.getStyleClass().add("main-menu-button");
        hostMultiplayerButton.setOnAction(event -> {
            playSound("click.wav");
            handleMainMenuInput("2");
        });

        Button joinMultiplayerButton = new Button("Join Multiplayer Game");
        joinMultiplayerButton.getStyleClass().add("main-menu-button");
        joinMultiplayerButton.setOnAction(event -> {
            playSound("click.wav");
            handleMainMenuInput("3");
        });

        Button addCaseButton = new Button("Add Custom Case");
        addCaseButton.getStyleClass().add("main-menu-button");
        addCaseButton.setOnAction(event -> {
            playSound("click.wav");
            handleMainMenuInput("4");
        });

        Button quitButton = new Button("Quit");
        quitButton.getStyleClass().add("main-menu-button");
        quitButton.setOnAction(event -> {
            playSound("click.wav");
            handleMainMenuInput("5");
        });

        mainMenuVBox
                .getChildren()
                .addAll(singlePlayerButton, hostMultiplayerButton, joinMultiplayerButton, addCaseButton, quitButton);
    }

    private void handleMainMenuInput(String input) {
        switch (input) {
            case "1":
                startSinglePlayer();
                break;
            case "2":
                startHostMultiplayer();
                break;
            case "3":
                startJoinMultiplayer();
                break;
            case "4":
                // For GUI, open the window. For terminal, prompt.
                if (System.getProperty("java.class.path").contains("openjfx")) { // A bit of a hack to detect GUI mode
                    new ui.windows.AddCaseWindow(this).show();
                } else {
                    currentState = UIState.ADDING_CASE_TERMINAL;
                    terminalTextArea.appendText("\nPlease enter the full file path to the case JSON file and press Enter:\n");
                }
                break;
            case "5":
                shutdown();
                break;
            default:
                terminalTextArea.appendText("Invalid selection. Please enter a number from 1 to 5.\n");
                break;
        }
    }

    private void updateUIVisibility() {
        Platform.runLater(() -> {
            Node currentView = roomPane.getChildren().isEmpty() ? null : roomPane.getChildren().get(0);
            Node nextView = null;

            switch (currentState) {
                case CHOOSING_CASE:
                case CHOOSING_LANGUAGE:
                    // These states manage their own views, but we need to ensure game buttons are off
                    tasksButton.setVisible(false);
                    journalButton.setVisible(false);
                    pinboardButton.setVisible(false);
                    chatButton.setVisible(false);
                    finalExamButton.setVisible(false);
                    helpButton.setVisible(false);
                    exitButton.setVisible(false);
                    rightInfoPanel.setVisible(false);
                    return; // Return early as the view is handled by show... methods
                case CASE_INVITATION:
                    tasksButton.setVisible(false);
                    journalButton.setVisible(false);
                    pinboardButton.setVisible(false);
                    chatButton.setVisible(false);
                    finalExamButton.setVisible(false);
                    helpButton.setVisible(false);
                    exitButton.setVisible(isHostPlayer); // Only host can exit at this stage
                    rightInfoPanel.setVisible(false);
                    return; // Return early to prevent view transition logic from running
                case MENU:
                    nextView = mainMenuVBox;
                    terminalTextArea.clear();
                    terminalTextArea.appendText("Welcome to Detective Game! Please select a mode to begin.\n");
                    terminalTextArea.appendText("\n--- Main Menu ---\n");
                    terminalTextArea.appendText("1. Single Player\n");
                    terminalTextArea.appendText("2. Host Multiplayer Game\n");
                    terminalTextArea.appendText("3. Join Multiplayer Game\n");
                    terminalTextArea.appendText("4. Add Custom Case\n");
                    terminalTextArea.appendText("5. Quit\n");
                    tasksButton.setVisible(false);
                    journalButton.setVisible(false);
                    pinboardButton.setVisible(false);
                    chatButton.setVisible(false);
                    finalExamButton.setVisible(false);
                    helpButton.setVisible(false);
                    exitButton.setVisible(false);
                    rightInfoPanel.setVisible(false);
                    break;
                case FINAL_EXAM:
                    nextView = finalExamView;
                    tasksButton.setVisible(true);
                    journalButton.setVisible(true);
                    pinboardButton.setVisible(true);
                    // Chat button should only be visible in multiplayer mode
                    chatButton.setVisible(!isSinglePlayer);
                    finalExamButton.setVisible(true);
                    helpButton.setVisible(true);
                    exitButton.setVisible(true);
                    rightInfoPanel.setVisible(true);
                    break;
                case JOIN_MENU_TERMINAL:
                    terminalTextArea.appendText("\n--- Join Multiplayer Game ---\n");
                    terminalTextArea.appendText("1. List public games\n");
                    terminalTextArea.appendText("2. Join by code\n");
                    terminalTextArea.appendText("3. Back to Main Menu\n");
                    terminalTextArea.appendText("---------------------------\n");
                    return; // Don't try to change the view
                case JOIN_GAME_MENU:
                    nextView = joinGameMenu;
                    tasksButton.setVisible(false);
                    journalButton.setVisible(false);
                    pinboardButton.setVisible(false);
                    chatButton.setVisible(false);
                    finalExamButton.setVisible(false);
                    helpButton.setVisible(false);
                    exitButton.setVisible(false);
                    rightInfoPanel.setVisible(false);
                    break;
                case MULTIPLAYER_MENU:
                    tasksButton.setVisible(false);
                    journalButton.setVisible(false);
                    pinboardButton.setVisible(false);
                    chatButton.setVisible(false);
                    finalExamButton.setVisible(false);
                    helpButton.setVisible(false);
                    exitButton.setVisible(false);
                    rightInfoPanel.setVisible(false);
                    return; // Return early to prevent view transition logic from running
                case GAME_SINGLE:
                    nextView = roomView;
                    tasksButton.setVisible(true);
                    journalButton.setVisible(true);
                    pinboardButton.setVisible(true);
                    chatButton.setVisible(false);
                    finalExamButton.setVisible(true);
                    helpButton.setVisible(true);
                    exitButton.setVisible(true);
                    rightInfoPanel.setVisible(true);
                    break;
                case GAME_MULTI:
                    nextView = roomView;
                    tasksButton.setVisible(true);
                    journalButton.setVisible(true);
                    pinboardButton.setVisible(true);
                    chatButton.setVisible(true);
                    finalExamButton.setVisible(true);
                    helpButton.setVisible(true);
                    exitButton.setVisible(true);
                    rightInfoPanel.setVisible(true);
                    break;
            }

            if (currentView != nextView) {
                final Node viewToDisplay = nextView;
                FadeTransition ft = new FadeTransition(Duration.millis(500), currentView);
                ft.setFromValue(1.0);
                ft.setToValue(0.0);
                ft.setOnFinished(event -> {
                    roomPane.getChildren().clear();
                    roomPane.getChildren().add(viewToDisplay);
                    FadeTransition ft2 = new FadeTransition(Duration.millis(500), viewToDisplay);
                    ft2.setFromValue(0.0);
                    ft2.setToValue(1.0);
                    ft2.play();
                });
                ft.play();
            }
        });
    }

    @Override
    public void onFinalExamUnlocked() {
        Platform.runLater(() -> {
            // finalExamButton is now always enabled, but we can still show a message if desired
            // terminalTextArea.appendText("\n--- Tasks Completed! ---\nYou may proceed to the Final Exam whenever you are ready.\n");
        });
    }

    private void startSinglePlayer() {
        isSinglePlayer = true;
        isHostPlayer = true;
        taskStates.clear();

        // Reset specialized controllers
        finalExamController = null;
        if (pinboardController != null) {
            pinboardController.reset();
        }

        // Clear the room view to prevent state bleeding from multiplayer
        updateRoomView(null);
        updateStatus("Starting Single Player...");
        currentState = UIState.CHOOSING_CASE;
        singlePlayerGame = new SinglePlayerMain();
        showSinglePlayerCaseSelection();
        checkIfAllTasksCompleted();
    }

    public void showSinglePlayerCaseSelection() {
        List<JsonDTO.CaseFile> cases = singlePlayerGame.getAvailableCases();
        VBox caseSelectionBox = new VBox(15);
        caseSelectionBox.setAlignment(Pos.CENTER);

        terminalTextArea.clear();
        terminalTextArea.appendText("--- Select a Case ---\n");
        for (int i = 0; i < cases.size(); i++) {
            JsonDTO.CaseFile caseFile = cases.get(i);
            terminalTextArea.appendText((i + 1) + ". " + caseFile.getUniversalTitle() + "\n");
            Button caseButton = new Button(caseFile.getUniversalTitle());
            caseButton.setOnAction(event -> showSinglePlayerLanguageSelection(caseFile));
            caseSelectionBox.getChildren().add(caseButton);
        }
        terminalTextArea.appendText("0. Back\n");
        terminalTextArea.appendText("---------------------\n");

        Button backButton = new Button("Back to Main Menu");
        backButton.setOnAction(event -> {
            currentState = UIState.MENU;
            updateUIVisibility();
        });

        caseSelectionBox.getChildren().add(backButton);
        roomPane.getChildren().clear();
        roomPane.getChildren().add(caseSelectionBox);
    }

    public void showCaseSelectionMenu() {
        Platform.runLater(() -> {
            if (taskStates != null) {
                taskStates.clear();
            }
            currentState = UIState.CHOOSING_CASE;
            updateUIVisibility();
            showSinglePlayerCaseSelection();
        });
    }

    public void returnToMultiplayerMenu() {
        Platform.runLater(() -> {
            // This will be called by the parser when the host cancels
            onMainMenu();
        });
    }

    public void returnToMainMenu() {
        manualDisconnect = true;
        GameClient.LaunchMode lastLaunchMode = gameClient != null ? gameClient.getLaunchMode() : null;

        // If we are hosting, delay the server shutdown slightly to ensure messages (like ReturnToLobbyDTO) are flushed to clients.
        if (embeddedServerRunning) {
            final server.GameServer serverToStop = embeddedServer;
            final Thread serverThreadToStop = embeddedServerThread;

            // Mark server as not running to prevent re-entry or new connections from UI perspective
            embeddedServerRunning = false;
            embeddedServer = null;
            embeddedServerThread = null;

            // Run the actual shutdown in a background thread with a delay
            new Thread(() -> {
                try {
                    // 1 second delay to allow broadcast messages to propagate
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    // Ignore interruption
                }

                Platform.runLater(() -> terminalTextArea.appendText("\nShutting down embedded server...\n"));
                if (serverToStop != null) {
                    serverToStop.stopServer();
                }

                if (serverThreadToStop != null && serverThreadToStop.isAlive()) {
                    try {
                        serverThreadToStop.join(1000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    if (serverThreadToStop.isAlive()) {
                        serverThreadToStop.interrupt();
                    }
                }
                Platform.runLater(() -> terminalTextArea.appendText("Embedded server shut down.\n"));
            }).start();
        } else {
            shutdownEmbeddedServer();
        }

        if (gameClient != null) {
            try {
                gameClient.stopClient();
            } catch (Exception e) {
                System.err.println("Error stopping client: " + e.getMessage());
            }
            gameClient = null;
        }
        if (gameClientThread != null) {
            if (gameClientThread.isAlive()) {
                gameClientThread.interrupt();
            }
            gameClientThread = null;
        }
        if (discoveryService instanceof UdpLanGameDiscoveryService) {
            ((UdpLanGameDiscoveryService) discoveryService).stop();
        }

        // Navigate to the appropriate menu based on launch mode
        if (lastLaunchMode == GameClient.LaunchMode.JOIN_ONLY) {
            currentState = UIState.JOIN_GAME_MENU;
            if (discoveryService instanceof UdpLanGameDiscoveryService) {
                ((UdpLanGameDiscoveryService) discoveryService).start();
            }
            refreshPublicGamesList();
        } else {
            currentState = UIState.MENU;
        }
        updateUIVisibility();
    }

    private void handleCaseSelectionInput(String input) {
        if (input.equals("0") || input.equalsIgnoreCase("back")) {
            currentState = UIState.MENU;
            updateUIVisibility();
            return;
        }
        try {
            int choice = Integer.parseInt(input);
            List<JsonDTO.CaseFile> cases = singlePlayerGame.getAvailableCases();
            if (choice > 0 && choice <= cases.size()) {
                showSinglePlayerLanguageSelection(cases.get(choice - 1));
            } else {
                terminalTextArea.appendText("Invalid selection. Please choose a valid case number.\n");
            }
        } catch (NumberFormatException e) {
            terminalTextArea.appendText("Invalid command. Please enter a number or '0' to go back.\n");
        }
    }

    private void showSinglePlayerLanguageSelection(JsonDTO.CaseFile caseFile) {
        this.selectedCaseFile = caseFile; // Store the selected case
        currentState = UIState.CHOOSING_LANGUAGE;
        VBox langSelectionBox = new VBox(15);
        langSelectionBox.setAlignment(Pos.CENTER);
        List<String> langCodes = new java.util.ArrayList<>(caseFile.getLocalizations().keySet());
        java.util.Collections.sort(langCodes);

        terminalTextArea.clear();
        terminalTextArea.appendText("--- Select a Language for " + caseFile.getUniversalTitle() + " ---\n");
        for (int i = 0; i < langCodes.size(); i++) {
            String langCode = langCodes.get(i);
            String langName = caseFile.getLocalizations().get(langCode).getLanguageName();
            terminalTextArea.appendText((i + 1) + ". " + langName + "\n");
            Button langButton = new Button(langName);
            langButton.setOnAction(event -> {
                JsonDTO.LocalizedCaseFile localizedCase = singlePlayerGame.selectCaseAndLanguage(caseFile, langCode);
                singlePlayerGame.initializeCase(localizedCase);
                showCaseInvitation(localizedCase.getInvitation(), true);
            });
            langSelectionBox.getChildren().add(langButton);
        }
        terminalTextArea.appendText("0. Back\n");
        terminalTextArea.appendText("-------------------------------------\n");
        Button backButton = new Button("Back to Case Selection");
        backButton.setOnAction(event -> {
            currentState = UIState.CHOOSING_CASE;
            showSinglePlayerCaseSelection();
        });
        langSelectionBox.getChildren().add(backButton);
        roomPane.getChildren().clear();
        roomPane.getChildren().add(langSelectionBox);
    }

    private void handleLanguageSelectionInput(String input) {
        if (input.equals("0") || input.equalsIgnoreCase("back")) {
            currentState = UIState.CHOOSING_CASE;
            showSinglePlayerCaseSelection();
            return;
        }
        try {
            int choice = Integer.parseInt(input);
            List<String> langCodes = new java.util.ArrayList<>(selectedCaseFile.getLocalizations().keySet());
            java.util.Collections.sort(langCodes);

            if (choice > 0 && choice <= langCodes.size()) {
                String langCode = langCodes.get(choice - 1);
                JsonDTO.LocalizedCaseFile localizedCase = singlePlayerGame.selectCaseAndLanguage(selectedCaseFile, langCode);
                singlePlayerGame.initializeCase(localizedCase);
                showCaseInvitation(localizedCase.getInvitation(), true);
            } else {
                terminalTextArea.appendText("Invalid selection. Please choose a valid language number.\n");
            }
        } catch (NumberFormatException e) {
            terminalTextArea.appendText("Invalid command. Please enter a number or '0' to go back.\n");
        }
    }

    private void startHostMultiplayer() {
        if (embeddedServerRunning) {
            terminalTextArea.appendText("\n[ERROR] An embedded server is already running.\n");
            return;
        }

        updateStatus("Starting embedded game server...");
        Platform.runLater(() -> terminalTextArea.appendText("\nStarting embedded game server for hosted multiplayer game...\n"));

        final java.util.concurrent.CountDownLatch startupLatch = new java.util.concurrent.CountDownLatch(1);
        final java.util.concurrent.atomic.AtomicBoolean serverStartedSuccessfully = new java.util.concurrent.atomic.AtomicBoolean(false);

        embeddedServerThread = new Thread(() -> {
            try {
                embeddedServer = new server.GameServer(NetworkConstants.DEFAULT_PORT);
                embeddedServer.startServer(); // This can throw BindException
                serverStartedSuccessfully.set(true);
                startupLatch.countDown(); // Signal success
                embeddedServer.run(); // This starts the server's main loop
            } catch (java.net.BindException e) {
                Platform.runLater(() -> terminalTextArea.appendText("\n[ERROR] Could not start embedded server (port " + NetworkConstants.DEFAULT_PORT + " already in use).\n"));
                startupLatch.countDown(); // Signal failure
            } catch (Exception e) {
                Platform.runLater(() -> terminalTextArea.appendText("\n[ERROR] Failed to start embedded server: " + e.getMessage() + "\n"));
                e.printStackTrace();
                startupLatch.countDown(); // Signal failure
            }
        }, "Embedded-GameServer-Thread");

        embeddedServerThread.setDaemon(true);
        embeddedServerThread.start();

        try {
            // Wait for the server to either start successfully or fail
            startupLatch.await(5, java.util.concurrent.TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            Platform.runLater(() -> terminalTextArea.appendText("\n[ERROR] Interrupted while waiting for server to start.\n"));
            return;
        }

        if (serverStartedSuccessfully.get()) {
            embeddedServerRunning = true;
            // Configure client to connect to the new embedded server
            this.configuredServerHost = "localhost";
            this.configuredServerPort = NetworkConstants.DEFAULT_PORT;
            startMultiplayer(GameClient.LaunchMode.HOST_ONLY, null);
        } else {
            // Server failed to start, cleanup and return to menu
            embeddedServer = null;
            embeddedServerThread = null;
            Platform.runLater(() -> {
                currentState = UIState.MENU;
                updateUIVisibility();
            });
        }
    }

    private void startJoinMultiplayer() {
        boolean isGuiMode = isGuiMode();

        if (discoveryService instanceof UdpLanGameDiscoveryService) {
            ((UdpLanGameDiscoveryService) discoveryService).start();
        }

        if (isGuiMode) {
            currentState = UIState.JOIN_GAME_MENU;
            updateUIVisibility();
            refreshPublicGamesList(); // Automatically refresh on showing the menu
        } else {
            // Terminal-based flow
            currentState = UIState.JOIN_MENU_TERMINAL;
            updateUIVisibility(); // This will print the join menu
        }
    }

    public void joinGameByDiscovery(DiscoveredGame game) {
        if (game == null) {
            terminalTextArea.appendText("\n[ERROR] Cannot join null game.\n");
            return;
        }
        this.configuredServerHost = game.getHostIp();
        this.configuredServerPort = game.getPort();
        startMultiplayer(GameClient.LaunchMode.JOIN_ONLY, game.getSessionId());
    }

    public void joinGameByCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            terminalTextArea.appendText("\n[ERROR] Join code cannot be empty.\n");
            return;
        }

        if (discoveryService instanceof UdpLanGameDiscoveryService) {
            java.util.Optional<DiscoveredGame> gameToJoin = ((UdpLanGameDiscoveryService) discoveryService).findByCode(code);

            if (gameToJoin.isPresent()) {
                DiscoveredGame game = gameToJoin.get();
                this.configuredServerHost = game.getHostIp();
                this.configuredServerPort = game.getPort();
                startMultiplayer(GameClient.LaunchMode.JOIN_ONLY, code);
            } else {
                terminalTextArea.appendText("\n[ERROR] No game with that code found on your local network.\n");
            }
        } else {
            // Fallback for stub or other implementations
            terminalTextArea.appendText("\n[INFO] Joining by code is only supported with the UDP discovery service.\n");
        }
    }

    private void handleJoinMenuTerminalInput(String input) {
        switch (input) {
            case "1":
                discoveryService.refreshAsync();
                java.util.List<DiscoveredGame> games = discoveryService.getCurrentGames();
                terminalTextArea.appendText("\n--- Public Games ---\n");
                if (games.isEmpty()) {
                    terminalTextArea.appendText("No public games found.\n");
                    currentState = UIState.JOIN_MENU_TERMINAL;
                    updateUIVisibility();
                } else {
                    for (int i = 0; i < games.size(); i++) {
                        terminalTextArea.appendText((i + 1) + ". " + games.get(i).toString() + "\n");
                    }
                    terminalTextArea.appendText("--------------------\n");
                    terminalTextArea.appendText("Enter number to join, or 0 to go back:\n");
                    currentState = UIState.LISTING_PUBLIC_GAMES_TERMINAL;
                }
                break;
            case "2":
                currentState = UIState.PROMPT_JOIN_CODE_TERMINAL;
                terminalTextArea.appendText("\nEnter join code: \n");
                break;
            case "3":
                currentState = UIState.MENU;
                updateUIVisibility();
                break;
            default:
                terminalTextArea.appendText("Invalid selection. Please enter a number from 1 to 3.\n");
                break;
        }
    }

    private void handlePublicGameSelectionTerminal(String input) {
        if ("0".equals(input)) {
            currentState = UIState.JOIN_MENU_TERMINAL;
            updateUIVisibility();
            return;
        }
        try {
            int gameIndex = Integer.parseInt(input) - 1;
            java.util.List<DiscoveredGame> games = discoveryService.getCurrentGames();
            if (gameIndex >= 0 && gameIndex < games.size()) {
                joinGameByDiscovery(games.get(gameIndex));
            } else {
                terminalTextArea.appendText("Invalid game number.\n");
            }
        } catch (NumberFormatException e) {
            terminalTextArea.appendText("Invalid input. Please enter a number.\n");
        }
    }

    private void startMultiplayer(GameClient.LaunchMode launchMode, String joinGameId) {
        isSinglePlayer = false;
        isHostPlayer = false; // Guest by default, updated by server
        manualDisconnect = false;
        taskStates.clear();

        if (pinboardController != null) {
            pinboardController.reset();
        }

        updateStatus("Starting Multiplayer Client...");

        // Use the configured host and port instead of launch args
        gameClient = new GameClient(configuredServerHost, configuredServerPort, this.taos, launchMode, joinGameId);
        gameClient.setListener(this);

        gameClientThread = new Thread(() -> {
            try {
                gameClient.run();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                // If this was a hosted game, shut down the server when the client finishes.
                if (launchMode == GameClient.LaunchMode.HOST_ONLY) {
                    shutdownEmbeddedServer();
                }

                // Only trigger automatic UI update if this wasn't a manual disconnect.
                // This prevents race conditions where returnToMainMenu() and this finally block
                // both try to update the UI simultaneously.
                if (!manualDisconnect) {
                    Platform.runLater(() -> {
                        // Ensure cleanup of client reference so the "Back" button works correctly
                        gameClient = null;
                        gameClientThread = null;

                        if (launchMode == GameClient.LaunchMode.JOIN_ONLY) {
                            currentState = UIState.JOIN_GAME_MENU;
                            if (discoveryService instanceof UdpLanGameDiscoveryService) {
                                ((UdpLanGameDiscoveryService) discoveryService).start();
                            }
                            refreshPublicGamesList();
                        } else {
                            currentState = UIState.MENU;
                        }
                        updateUIVisibility();
                    });
                }
            }
        }, "GameClient-Thread");
        gameClientThread.setDaemon(true);
        gameClientThread.start();
        currentState = UIState.GAME_MULTI;
        // Don't call updateUIVisibility here, the listener will do it.
    }


    private String getLaunchArg(int index, String defaultValue) {
        if (launchArgs != null && launchArgs.size() > index) {
            return launchArgs.get(index);
        }
        return defaultValue;
    }

    private int getLaunchArg(int index, int defaultValue) {
        if (launchArgs != null && launchArgs.size() > index) {
            try {
                return Integer.parseInt(launchArgs.get(index));
            } catch (NumberFormatException e) {
                // Ignore
            }
        }
        return defaultValue;
    }

    public void shutdownEmbeddedServer() {
        if (embeddedServerRunning && embeddedServer != null) {
            Platform.runLater(() -> terminalTextArea.appendText("\nShutting down embedded server...\n"));

            embeddedServer.stopServer(); // Signal the server to stop
            if (embeddedServerThread != null && embeddedServerThread.isAlive()) {
                try {
                    // Give the server a moment to close connections
                    embeddedServerThread.join(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    Platform.runLater(() -> terminalTextArea.appendText("\n[WARN] Interrupted while waiting for server thread to shut down.\n"));
                }
                if (embeddedServerThread.isAlive()) {
                    embeddedServerThread.interrupt(); // Forcefully interrupt if it's stuck
                }
            }
            embeddedServerRunning = false;
            embeddedServer = null;
            embeddedServerThread = null;
            Platform.runLater(() -> terminalTextArea.appendText("Embedded server shut down.\n"));
        }
    }

    public void shutdown() {
        if (discoveryService instanceof UdpLanGameDiscoveryService) {
            ((UdpLanGameDiscoveryService) discoveryService).stop();
        }
        shutdownEmbeddedServer(); // Ensure server is stopped on app exit
        System.out.println("\nShutting down application...");
        if (gameClient != null) {
            gameClient.stopClient();
        }
        if (gameClientThread != null && gameClientThread.isAlive()) {
            try {
                gameClientThread.join(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        Platform.exit();
        System.exit(0);
    }

    @FXML
    private void handleTerminalInput() {
        String input = terminalInputField.getText().trim();
        if (!input.isEmpty()) {
            if (currentState == UIState.GAME_SINGLE) {
                singlePlayerGame.processCommand(input);
            } else if ((currentState == UIState.GAME_MULTI || currentState == UIState.MULTIPLAYER_MENU) && gameClient != null) {
                if (currentMultiplayerSubState == UIMultiplayerSubState.MAIN_MENU && input.equals("3")) {
                    gameClient.stopClient();
                    if (gameClientThread != null) {
                        gameClientThread.interrupt(); // Interrupt the thread to unblock it
                    }
                } else {
                    gameClient.enqueueUserInput(input);
                }
            } else if (currentState == UIState.MENU) {
                handleMainMenuInput(input);
            } else if (currentState == UIState.CHOOSING_CASE) {
                handleCaseSelectionInput(input);
            } else if (currentState == UIState.CHOOSING_LANGUAGE) {
                handleLanguageSelectionInput(input);
            } else if (currentState == UIState.ADDING_CASE_TERMINAL) {
                String result = singleplayer.util.CaseFileUtil.addCaseFile(input);
                terminalTextArea.appendText(result + "\n");
                currentState = UIState.MENU;
                updateUIVisibility(); // To reprint the main menu
            } else if (currentState == UIState.JOIN_MENU_TERMINAL) {
                handleJoinMenuTerminalInput(input);
            } else if (currentState == UIState.LISTING_PUBLIC_GAMES_TERMINAL) {
                handlePublicGameSelectionTerminal(input);
            } else if (currentState == UIState.PROMPT_JOIN_CODE_TERMINAL) {
                joinGameByCode(input);
                currentState = UIState.MENU; // Go back to main menu after trying to join
                updateUIVisibility();
            } else if (currentState == UIState.CASE_INVITATION && !isSinglePlayer) {
                gameClient.enqueueUserInput(input);
            } else if (currentState == UIState.CASE_INVITATION && isSinglePlayer) {
                if (input.equalsIgnoreCase("start case")) {
                    handleStartCase();
                }
            } else if (currentState == UIState.FINAL_EXAM) {
                // Handle commands during final exam view (e.g. if exam finished)
                if (input.equals("1")) {
                    exitFinalExamMode();
                } else if (input.equals("2")) {
                    boolean solved = false;
                    if (isSinglePlayer && singlePlayerGame != null && singlePlayerGame.getGameContext().getLastResultDTO() != null) {
                        solved = singlePlayerGame.getGameContext().getLastResultDTO().isCaseSolved();
                    } else if (!isSinglePlayer && lastExamResult != null) {
                        solved = lastExamResult.isCaseSolved();
                    }

                    if (solved) {
                        if (isSinglePlayer) {
                            returnToMainMenu();
                        } else if (gameClient != null) {
                            gameClient.enqueueUserInput("exit");
                        }
                    } else {
                        terminalTextArea.appendText("Invalid selection.\n");
                    }
                } else {
                    // Pass through to standard command processor (e.g. for "look", "help")
                    if (isSinglePlayer) {
                        singlePlayerGame.processCommand(input);
                    } else if (gameClient != null) {
                        gameClient.enqueueUserInput(input);
                    }
                }
            }
            terminalInputField.clear();
        }
    }

    private void openTasksWindow() {
        if (tasksWindow == null) {
            tasksWindow = new TasksWindow(this);
        }

        // Dynamically load tasks based on the current game context
        if (isSinglePlayer && singlePlayerGame != null) {
            List<String> tasks = singlePlayerGame.getCurrentCaseTasks();
            tasksWindow.loadTasks(tasks, taskStates);
        } else if (!isSinglePlayer && gameClient != null) {
            List<String> tasks = gameClient.getCurrentCaseTasks();
            tasksWindow.loadTasks(tasks, taskStates);
        }

        tasksWindow.show();
    }

    public void updateTaskState(String task, boolean isCompleted) {
        if (isSinglePlayer) {
            // For single player, we just update the local map directly.
            taskStates.put(task, isCompleted);
            checkIfAllTasksCompleted();
        } else if (gameClient != null) {
            // For multiplayer, we need to find the task index and send a command.
            List<String> tasks = gameClient.getCurrentCaseTasks();
            if (tasks != null) {
                int taskIndex = tasks.indexOf(task);
                if (taskIndex != -1) {
                    // Note: We are NOT updating the local map here directly.
                    // The UI will only update when the server broadcasts the change back to us,
                    // ensuring a single source of truth and synchronization.
                    UpdateTaskStateCommand command = new UpdateTaskStateCommand(taskIndex, isCompleted);
                    gameClient.sendDirectCommand(command);
                }
            }
        }
    }

    public void exitFinalExamMode() {
        exitFinalExamUI();
        if (isSinglePlayer) {
            singlePlayerGame.processCommand("look");
        } else if (gameClient != null) {
            // For multiplayer, we send the specific ContinueGameCommand.
            // Since we don't have a text command parser for this, we send the object directly.
            gameClient.sendDirectCommand(new common.commands.ContinueGameCommand());
        }
    }

    private void exitFinalExamUI() {
        Platform.runLater(() -> {
            if (isSinglePlayer) {
                currentState = UIState.GAME_SINGLE;
            } else {
                currentState = UIState.GAME_MULTI;
            }
            updateUIVisibility();
            refreshRoomView();
        });
    }

    private void openJournalWindow() {
        if (journalWindow == null) {
            journalWindow = new JournalWindow(this);
        }

        // Dynamically load journal entries based on the current game context
        if (isSinglePlayer && singlePlayerGame != null) {
            List<common.dto.JournalEntryDTO> entries = singlePlayerGame.getGameContext().getJournalEntries(null);
            if (entries != null) {
                journalWindow.setEntries(entries);
            }
        } else if (!isSinglePlayer && gameClient != null) {
            List<common.dto.JournalEntryDTO> entries = gameClient.getJournalEntries();
            if (entries != null) {
                journalWindow.setEntries(entries);
            }
        }

        journalWindow.show();
    }

    private void openPinboardWindow() {
        if (pinboardController == null) {
            System.out.println("Initializing Pinboard Controller...");
            pinboardController = new PinboardController();
        }

        // Load existing clues if just opening for first time but game has progressed
        syncPinboardData();

        pinboardController.setOnSyncRequest(this::syncPinboardData);
        pinboardController.show();
    }

    private void syncPinboardData() {
        if (pinboardController == null) return;

        if (isSinglePlayer && singlePlayerGame != null) {
            List<common.dto.JournalEntryDTO> entries = singlePlayerGame.getGameContext().getJournalEntries(null);
            if (entries != null) {
                for (common.dto.JournalEntryDTO entry : entries) {
                    pinboardController.addJournalEntry(entry);
                }
            }
        } else if (!isSinglePlayer && gameClient != null) {
            List<common.dto.JournalEntryDTO> entries = gameClient.getJournalEntries();
            if (entries != null) {
                for (common.dto.JournalEntryDTO entry : entries) {
                    pinboardController.addJournalEntry(entry);
                }
            }
        }
    }

    private void openChatWindow() {
        if (chatWindow == null) {
            chatWindow = new ChatWindow(this);
        }

        // Dynamically load chat history
        if (!isSinglePlayer && gameClient != null) {
            List<common.dto.ChatMessage> history = gameClient.getChatHistory();
            if (history != null) {
                chatWindow.loadHistory(history);
            }
        }

        chatWindow.show();
        unreadChatCount = 0;
        updateUnreadChatLabel();
    }

    private void openHelpWindow() {
        if (helpWindow == null) {
            helpWindow = new HelpWindow();
        }
        helpWindow.show();
    }

    public void incrementUnreadChat() {
        unreadChatCount++;
        updateUnreadChatLabel();
    }

    private void updateUnreadChatLabel() {
        if (unreadChatCount > 0) {
            unreadChatLabel.setText(String.valueOf(unreadChatCount));
            unreadChatLabel.setVisible(true);
        } else {
            unreadChatLabel.setVisible(false);
        }
    }

    public void updateStatus(String status) {
        if (statusLabel != null) {
            statusLabel.setText(status);
        }
    }

    public TextArea getTerminalTextArea() {
        return terminalTextArea;
    }

    public StackPane getRoomPane() {
        return roomPane;
    }

    public VBox getRightInfoPanel() {
        return rightInfoPanel;
    }

    public void sendCommand(String command) {
        if ((currentState == UIState.GAME_MULTI
                || currentState == UIState.MULTIPLAYER_MENU
                || currentState == UIState.FINAL_EXAM
                || (currentState == UIState.CASE_INVITATION && !isSinglePlayer))
                && gameClient != null) {
            gameClient.enqueueUserInput(command);
        } else if ((currentState == UIState.GAME_SINGLE || currentState == UIState.FINAL_EXAM || (currentState == UIState.CASE_INVITATION && isSinglePlayer)) && singlePlayerGame != null) {
            singlePlayerGame.processCommand(command);
        }
    }

    public GameClient getGameClient() {
        return gameClient;
    }

    public void updateRoomView(RoomDescriptionDTO roomDescription) {
        if (roomView != null) {
            Platform.runLater(() -> {
                if (roomDescription != null) {
                    roomView.loadRoom(roomDescription);
                    updateRightPanel(roomDescription);
                    updateStatus("Current room: " + roomDescription.getName());
                } else {
                    roomView.clear();
                    updateRightPanel(null);
                    updateStatus("No active game.");
                }
            });
        }
    }

    public void refreshRoomView() {
        if (isSinglePlayer && singlePlayerGame != null && singlePlayerGame.getGameContext() != null) {
            singleplayer.GameContextSinglePlayer context = singlePlayerGame.getGameContext();
            Core.Room currentRoom = context.getCurrentRoomForPlayer(null);
            if (currentRoom != null) {
                RoomDescriptionDTO dto = context.createRoomDescriptionDTO(currentRoom, null);
                updateRoomView(dto);
            }
        }
    }

    public void refreshJournalWindow() {
        if (journalWindow != null) {
            Platform.runLater(this::openJournalWindow);
        }
        // Also sync Pinboard if active
        Platform.runLater(() -> {
            if (pinboardController != null) {
                syncPinboardData();
            }
        });
    }

    private void updateRightPanel(RoomDescriptionDTO roomDescription) {
        neighboringRoomsContainer.getChildren().clear();

        if (roomDescription == null || roomDescription.getExits() == null) {
            return;
        }

        for (java.util.Map.Entry<String, String> entry : roomDescription.getExits().entrySet()) {
            String direction = entry.getKey();
            String roomName = entry.getValue();
            String buttonText = direction + ": " + roomName;

            Button roomButton = new Button(buttonText);
            roomButton.setPrefWidth(Double.MAX_VALUE); // Make buttons fill the width

            roomButton.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2) {
                    // Command is the direction, e.g., "north"
                    String command = "move " + direction;
                    sendCommand(command);
                }
            });

            Tooltip tooltip = new Tooltip("Double-click to move to the " + roomName);
            roomButton.setTooltip(tooltip);

            neighboringRoomsContainer.getChildren().add(roomButton);
        }
    }

    public void showRoomResponse(String targetName, String response) {
        if (roomView != null) {
            Platform.runLater(() -> {
                roomView.showResponseBubble(targetName, response);
            });
        }
    }

    public void addJournalEntry(String entry) {
        if (journalWindow != null) {
            Platform.runLater(() -> {
                journalWindow.addEntry(entry);
            });
        }
    }

    public void addChatMessage(String sender, String message) {
        if (chatWindow != null) {
            Platform.runLater(() -> {
                chatWindow.addChatMessage(sender, message);
            });
        }
    }

    @Override
    public void onDisconnected() {
        if (gameClient != null && gameClient.getLaunchMode() == GameClient.LaunchMode.HOST_ONLY) {
            // If the host disconnects, it's usually because they are shutting down.
            // Go directly back to the main menu.
            Platform.runLater(this::returnToMainMenu);
            return;
        }

        currentMultiplayerSubState = UIMultiplayerSubState.DISCONNECTED;
        Platform.runLater(() -> {
            VBox disconnectedBox = new VBox(15);
            disconnectedBox.setAlignment(Pos.CENTER);
            Label label = new Label("Disconnected from server.");
            Button reconnectButton = new Button("Reconnect");
            reconnectButton.setOnAction(event -> sendCommand("connect"));
            Button mainMenuButton = new Button("Back to Main Menu");
            mainMenuButton.setOnAction(event -> returnToMainMenu());
            disconnectedBox.getChildren().addAll(label, reconnectButton, mainMenuButton);
            roomPane.getChildren().clear();
            roomPane.getChildren().add(disconnectedBox);
        });
    }

    @Override
    public void onConnecting() {
        currentMultiplayerSubState = UIMultiplayerSubState.CONNECTING;
        Platform.runLater(() -> {
            VBox connectingBox = new VBox(15);
            connectingBox.setAlignment(Pos.CENTER);
            Label label = new Label("Connecting to server...");
            connectingBox.getChildren().add(label);
            roomPane.getChildren().clear();
            roomPane.getChildren().add(connectingBox);
        });
    }

    @Override
    public void onConnected() {
        // This will shortly be followed by onMainMenu
    }

    @Override
    public void onReturnToMainMenu(String message) {
        Platform.runLater(() -> {
            terminalTextArea.appendText("\n" + message + "\n");
            returnToMainMenu();
        });
    }

    @Override
    public void onMainMenu() {
        if (taskStates != null) {
            taskStates.clear();
        }
        // This method is now a router. Based on the launch mode, it will either
        // show host options or join options, bypassing the old multiplayer menu.
        if (gameClient != null && gameClient.getLaunchMode() == GameClient.LaunchMode.HOST_ONLY) {
            onHostGameOptions();
        } else {
            // Default to join options for NORMAL and JOIN_ONLY modes in the GUI flow
            onJoinGameOptions();
        }
    }

    @Override
    public void onHostGameOptions() {
        currentMultiplayerSubState = UIMultiplayerSubState.HOST_OPTIONS;
        currentState = UIState.MULTIPLAYER_MENU;
        updateUIVisibility();
        Platform.runLater(() -> {
            terminalTextArea.clear();
            terminalTextArea.appendText("--- Host Game Options ---\n");
            terminalTextArea.appendText("1. Host Public Game\n");
            terminalTextArea.appendText("2. Host Private Game\n");
            terminalTextArea.appendText("3. Back\n");
            terminalTextArea.appendText("-------------------------\n");
            VBox hostOptionsBox = new VBox(15);
            hostOptionsBox.setAlignment(Pos.CENTER);
            Button publicButton = new Button("Host Public Game");
            publicButton.setOnAction(event -> sendCommand("1"));
            Button privateButton = new Button("Host Private Game");
            privateButton.setOnAction(event -> sendCommand("2"));
            Button backButton = new Button("Back");
            backButton.setOnAction(event -> sendCommand("3"));
            hostOptionsBox.getChildren().addAll(publicButton, privateButton, backButton);
            roomPane.getChildren().clear();
            roomPane.getChildren().add(hostOptionsBox);
        });
    }

    @Override
    public void onCaseSelection(List<JsonDTO.CaseFile> cases) {
        currentMultiplayerSubState = UIMultiplayerSubState.CASE_SELECTION;
        currentState = UIState.MULTIPLAYER_MENU;
        updateUIVisibility();
        Platform.runLater(() -> {
            terminalTextArea.clear();
            terminalTextArea.appendText("--- Select a Case ---\n");
            VBox caseSelectionBox = new VBox(15);
            caseSelectionBox.setAlignment(Pos.CENTER);
            for (int i = 0; i < cases.size(); i++) {
                final int caseNum = i + 1;
                String caseTitle = cases.get(i).getUniversalTitle();
                terminalTextArea.appendText(caseNum + ". " + caseTitle + "\n");
                Button caseButton = new Button(caseTitle);
                caseButton.setOnAction(event -> sendCommand(String.valueOf(caseNum)));
                caseSelectionBox.getChildren().add(caseButton);
            }
            terminalTextArea.appendText("0. Back\n");
            terminalTextArea.appendText("---------------------\n");
            Button backButton = new Button("Back");
            backButton.setOnAction(event -> sendCommand("0"));
            caseSelectionBox.getChildren().add(backButton);
            roomPane.getChildren().clear();
            roomPane.getChildren().add(caseSelectionBox);
        });
    }

    @Override
    public void onLanguageSelection(JsonDTO.CaseFile caseFile) {
        currentMultiplayerSubState = UIMultiplayerSubState.LANGUAGE_SELECTION;
        currentState = UIState.MULTIPLAYER_MENU;
        updateUIVisibility();
        Platform.runLater(() -> {
            terminalTextArea.clear();
            terminalTextArea.appendText("--- Select a Language for " + caseFile.getUniversalTitle() + " ---\n");
            VBox langSelectionBox = new VBox(15);
            langSelectionBox.setAlignment(Pos.CENTER);
            List<String> langCodes = new java.util.ArrayList<>(caseFile.getLocalizations().keySet());
            java.util.Collections.sort(langCodes);
            for (int i = 0; i < langCodes.size(); i++) {
                final int langNum = i + 1;
                String langCode = langCodes.get(i);
                String langName = caseFile.getLocalizations().get(langCode).getLanguageName();
                terminalTextArea.appendText(langNum + ". " + langName + "\n");
                Button langButton = new Button(langName);
                langButton.setOnAction(event -> sendCommand(String.valueOf(langNum)));
                langSelectionBox.getChildren().add(langButton);
            }
            terminalTextArea.appendText("0. Back\n");
            terminalTextArea.appendText("-------------------------------------\n");
            Button backButton = new Button("Back");
            backButton.setOnAction(event -> sendCommand("0"));
            langSelectionBox.getChildren().add(backButton);
            roomPane.getChildren().clear();
            roomPane.getChildren().add(langSelectionBox);
        });
    }

    @Override
    public void onHostingLobby(String gameCode) {
        currentMultiplayerSubState = UIMultiplayerSubState.HOSTING_LOBBY;
        Platform.runLater(() -> {
            VBox lobbyBox = new VBox(15);
            lobbyBox.setAlignment(Pos.CENTER);
            Label label = new Label("Waiting for another player to join...");
            if (gameCode != null) {
                Label codeLabel = new Label("Private Game Code: " + gameCode);
                lobbyBox.getChildren().add(codeLabel);
            }
            Button cancelButton = new Button("Cancel");
            cancelButton.setOnAction(event -> sendCommand("cancel"));
            lobbyBox.getChildren().addAll(label, cancelButton);
            roomPane.getChildren().clear();
            roomPane.getChildren().add(lobbyBox);
        });
    }

    @Override
    public void onJoinGameOptions() {
        currentMultiplayerSubState = UIMultiplayerSubState.JOIN_OPTIONS;

        // For GUI mode, immediately switch to the unified join menu and refresh.
        if (isGuiMode()) {
            currentState = UIState.JOIN_GAME_MENU;
            updateUIVisibility();
            refreshPublicGamesList(); // Automatically refresh
            return; // Skip terminal-specific logic
        }

        // For terminal mode, display the text-based options.
        Platform.runLater(() -> {
            terminalTextArea.clear();
            terminalTextArea.appendText("--- Join Game Options ---\n");
            terminalTextArea.appendText("1. Join Public Game\n");
            terminalTextArea.appendText("2. Join Private Game\n");
            terminalTextArea.appendText("3. Back\n");
            terminalTextArea.appendText("-----------------------\n");
        });
    }

    @Override
    public void onPublicGamesList(List<PublicGameInfoDTO> games) {
        currentMultiplayerSubState = UIMultiplayerSubState.PUBLIC_GAMES_LIST;
        currentState = UIState.MULTIPLAYER_MENU;
        updateUIVisibility();
        Platform.runLater(() -> {
            terminalTextArea.clear();
            terminalTextArea.appendText("--- Public Games ---\n");
            VBox gamesBox = new VBox(15);
            gamesBox.setAlignment(Pos.CENTER);
            for (int i = 0; i < games.size(); i++) {
                final int gameNum = i + 1;
                PublicGameInfoDTO game = games.get(i);
                String gameInfo = game.getCaseTitle() + " hosted by " + game.getHostPlayerDisplayId();
                terminalTextArea.appendText(gameNum + ". " + gameInfo + "\n");
                Button gameButton = new Button(gameInfo);
                gameButton.setOnAction(event -> sendCommand(String.valueOf(gameNum)));
                gamesBox.getChildren().add(gameButton);
            }
            terminalTextArea.appendText("0. Back\n");
            terminalTextArea.appendText("--------------------\n");
            Button backButton = new Button("Back");
            backButton.setOnAction(event -> sendCommand("0"));
            gamesBox.getChildren().add(backButton);
            roomPane.getChildren().clear();
            roomPane.getChildren().add(gamesBox);
        });
    }

    @Override
    public void onPrivateGameEntry() {
        currentMultiplayerSubState = UIMultiplayerSubState.PRIVATE_GAME_ENTRY;
        currentState = UIState.MULTIPLAYER_MENU;
        updateUIVisibility();
        Platform.runLater(() -> {
            VBox privateGameBox = new VBox(15);
            privateGameBox.setAlignment(Pos.CENTER);
            Label label = new Label("Enter Private Game Code:");
            TextField codeField = new TextField();
            codeField.setOnAction(event -> sendCommand(codeField.getText()));
            Button backButton = new Button("Back");
            backButton.setOnAction(event -> sendCommand("cancel"));
            privateGameBox.getChildren().addAll(label, codeField, backButton);
            roomPane.getChildren().clear();
            roomPane.getChildren().add(privateGameBox);
        });
    }

    @Override
    public void onLobby() {
        currentMultiplayerSubState = UIMultiplayerSubState.IN_LOBBY;
    }

    @Override
    public void onEnterGame(RoomDescriptionDTO initialRoom) {
        // Wrap everything to ensure thread safety for UI component creation (PinboardController)
        Platform.runLater(() -> {
            isSinglePlayer = false;
            currentMultiplayerSubState = UIMultiplayerSubState.IN_GAME;
            currentState = UIState.GAME_MULTI;

            // Initialize Pinboard networking - safe to call here because we are inside Platform.runLater
            if (gameClient != null) {
                initializePinboardNetworking();
                gameClient.sendPinboardStateRequest();
            }

            // Instantiate a fresh RoomView to ensure no stale state from previous games
            roomView = new RoomView(this);
            roomPane.getChildren().clear();
            roomPane.getChildren().add(roomView);
            tasksButton.setVisible(true);
            journalButton.setVisible(true);
            pinboardButton.setVisible(true);
            chatButton.setVisible(true);
            finalExamButton.setVisible(true);
            finalExamButton.setDisable(false);
            helpButton.setVisible(true);
            exitButton.setVisible(true);
            rightInfoPanel.setVisible(true);
            updateRoomView(initialRoom);
        });
    }

    private void initializePinboardNetworking() {
        if (pinboardController == null) {
            pinboardController = new PinboardController();
        }

        // 1. Outgoing updates: Pinboard -> Server
        pinboardController.setOnUpdateCallback(update -> {
            if (gameClient != null) {
                gameClient.sendPinboardUpdate(update);
            }
        });

        // 2. Incoming updates: Server -> Pinboard (Wrapped in runLater to be safe)
        gameClient.setPinboardUpdateListener(update -> {
            if (pinboardController != null) {
                Platform.runLater(() -> pinboardController.applyUpdate(update));
            }
        });

        // 3. Initial State: Server -> Pinboard (Wrapped in runLater to be safe)
        gameClient.setPinboardStateListener(state -> {
            if (pinboardController != null) {
                Platform.runLater(() -> pinboardController.applyState(state));
            }
        });
    }

    @Override
    public void onUpdateRoom(RoomDescriptionDTO newRoom) {
        updateRoomView(newRoom);
        // If we are in the Final Exam view (e.g., as a guest waiting for host),
        // and we receive a room update (host pressed Continue), we should exit the exam UI.
        if (currentState == UIState.FINAL_EXAM) {
            exitFinalExamUI();
        }
    }

    @Override
    public void onJoinGameFailed(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Join Game Failed");
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();

            // Return to the join menu and refresh the list
            currentState = UIState.JOIN_GAME_MENU;
            updateUIVisibility();
            refreshPublicGamesList();
        });
    }

    @Override
    public void onReceiveCaseInvitation(String invitation, boolean isHost) {
        this.isHostPlayer = isHost;
        showCaseInvitation(invitation, isHost);
    }

    @Override
    public void onJournalUpdated() {
        refreshJournalWindow();
    }

    @Override
    public void onChatMessageReceived(common.dto.ChatMessage message) {
        if (chatWindow != null) {
            Platform.runLater(() -> {
                chatWindow.addChatMessage(message);
            });
        }
    }

    @Override
    public void onTaskStateUpdate(int taskIndex, boolean isCompleted) {
        // This is a multiplayer-only feature, as single player state is local.
        if (isSinglePlayer || gameClient == null) {
            return;
        }

        Platform.runLater(() -> {
            List<String> tasks = gameClient.getCurrentCaseTasks();

            if (tasks != null && taskIndex >= 0 && taskIndex < tasks.size()) {
                String task = tasks.get(taskIndex);
                this.taskStates.put(task, isCompleted); // Update the local state map

                // If the tasks window is open, refresh its view to reflect the change
                if (tasksWindow != null && tasksWindow.isShowing()) {
                    tasksWindow.loadTasks(tasks, this.taskStates);
                }
            }
        });
    }

    private boolean isGuiMode() {
        return taos != null;
    }

    private void checkIfAllTasksCompleted() {
        // Logic removed as Final Exam is no longer gated by task completion.
        // We could keep it to show a "Case Solved" message or similar, but the button unlock is irrelevant.
    }

    @Override
    public void showQuestion(ExamQuestionDTO questionDTO) {
        finalExamViewController.displayQuestion(questionDTO);
    }

    @Override
    public void showExamResults(ExamResultDTO resultDTO) {
        this.lastExamResult = resultDTO;
        finalExamViewController.displayResults(resultDTO);
    }

    @Override
    public void showExamView() {
        currentState = UIState.FINAL_EXAM;
        if (!isSinglePlayer && gameClient != null) {
            this.finalExamController = gameClient.getFinalExamController();
        }
        finalExamViewController.setFinalExamController(finalExamController, isHostPlayer);
        updateUIVisibility();

        // Ensure chat is visible during exam in multiplayer
        if (!isSinglePlayer) {
            chatButton.setVisible(true);
        }
    }

    @Override
    public void notifyUnansweredQuestions() {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Unanswered Questions");
            alert.setHeaderText(null);
            alert.setContentText("Please answer all questions before submitting the exam.");
            alert.showAndWait();
        });
    }

    @Override
    public void onFinalExamRequest(String requesterDisplayName) {
        Platform.runLater(() -> {
            terminalTextArea.appendText("\n" + requesterDisplayName + " has requested to start the Final Exam.\n");
        });
    }

    public void onSinglePlayerExamStarted() {
        Platform.runLater(() -> {
            currentState = UIState.FINAL_EXAM;
            // In SP, we don't have a separate controller instance managed by GameClient usually.
            // But SinglePlayerMain manages context.
            // We need to wire up the ViewController to act on SinglePlayerMain.
            // However, FinalExamViewController expects a FinalExamController (Client logic).
            // We might need a bridge or just direct calls if we want to reuse the view.

            // Actually, GameClient logic isn't used in SP. We need to adapt FinalExamViewController or provide a dummy controller?
            // Ideally, we should use FinalExamController in SP too, but it depends on GameClient.
            // Let's create a FinalExamController that talks to SinglePlayerMain instead of GameClient?
            // Or just implement the interface methods `showQuestion` etc which MainController already does.

            // If we just switch the view, `finalExamViewController` needs data.
            // `onSinglePlayerQuestionUpdate` will provide data.

            // We also need to hook up the "Submit" button in FinalExamViewController to call SinglePlayerMain.
            // `finalExamViewController.setFinalExamController` expects `client.exam.FinalExamController`.
            // This is a tight coupling problem. `client.exam.FinalExamController` sends commands via `GameClient`.
            // In SP, we want to call `singlePlayerGame.processCommand("submit exam answer ...")` or similar.

            // QUICK FIX: Create a shim/adapter for FinalExamController that delegates to SinglePlayerMain.
            // Since FinalExamController is a class, we can't easily interface it out without refactoring.
            // But `FinalExamViewController` calls methods on `FinalExamController`.

            // Alternative: Update FinalExamViewController to use an interface `FinalExamInputHandler`.
            // But I can't refactor everything now.

            // Simplest: `onSinglePlayerQuestionUpdate` will push DTO to view.
            // `FinalExamViewController` needs to know how to submit.
            // I will make a `SinglePlayerFinalExamController` extending `FinalExamController`? No, final class? No.
            // Let's look at FinalExamController again. It takes GameClient.

            // I will handle this by fetching the DTO and calling displayQuestion.
            // BUT the view needs to send input back.
            // I will set a special `FinalExamController` that has a null GameClient but overrides submission?
            // `FinalExamController` logic is: `gameClient.sendDirectCommand(...)`.

            // Let's defer the input wiring to `onSinglePlayerQuestionUpdate`.

            updateUIVisibility();
        });
        // Trigger immediate update if question is ready
        onSinglePlayerQuestionUpdate();
    }

    public void onSinglePlayerQuestionUpdate() {
        if (singlePlayerGame == null || singlePlayerGame.getGameContext() == null) return;

        ExamQuestionDTO dto = singlePlayerGame.getGameContext().getCurrentExamQuestionDTO();
        if (dto != null) {
            // We need to set up the controller for the view if not set.
            // We can pass a dummy GameClient? No, that will cause NPEs.
            // We need a valid controller.
            if (finalExamController == null) {
                // Retrieve the FinalExamDTO from the single player context
                common.dto.FinalExamDTO examDTO = singlePlayerGame.getGameContext().getSelectedCase().getFinalExam();

                finalExamController = new FinalExamController(examDTO, this, null, true) {
                    @Override
                    public void submitCurrentQuestion(java.util.Map<String, String> answers) {
                        common.dto.ExamQuestionDTO currentQ = singlePlayerGame.getGameContext().getCurrentExamQuestionDTO();
                        if (currentQ != null) {
                            singlePlayerGame.getGameContext().processSubmitQuestionAnswer(null,
                                    currentQ.getQuestionIndex(),
                                    answers);
                        } else {
                            // Exam might be finished or already processed.
                            // Check if result is available to display?
                            // onSinglePlayerExamResult() should have been triggered.
                            // Suppress error message to avoid confusing user if this is a race condition.
                        }
                    }
                };
            }

            finalExamViewController.setFinalExamController(finalExamController, true);
            finalExamViewController.displayQuestion(dto);
        }
    }

    public void onSinglePlayerExamResult() {
        if (singlePlayerGame == null || singlePlayerGame.getGameContext() == null) return;
        ExamResultDTO dto = singlePlayerGame.getGameContext().getLastResultDTO();
        if (dto != null) {
            this.lastExamResult = dto;
            finalExamViewController.displayResults(dto);
        }
    }
}