package ui.windows;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import ui.MainController;

import common.dto.ChatMessage;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Chat window for the Detective Game multiplayer mode.
 * Allows players to communicate with each other during the game.
 */
public class ChatWindow {

  private Stage stage;
  private MainController mainController;
  private ListView<String> chatListView;
  private TextField chatInputField;
  private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

  public ChatWindow(MainController controller) {
    this.mainController = controller;
    initializeWindow();
  }

  private void initializeWindow() {
    stage = new Stage();
    stage.setTitle("Chat");

    BorderPane root = new BorderPane();
    root.setPadding(new Insets(10));
    root.setStyle("-fx-background-color: #1a1a1a;");

    VBox centerBox = new VBox(5);
    
    Label chatLabel = new Label("Chat History:");
    chatLabel.setStyle("-fx-text-fill: #d4af37; -fx-font-weight: bold; -fx-font-size: 14;");
    
    chatListView = new ListView<>();
    chatListView.setStyle("-fx-background-color: #0a0a0a; -fx-control-inner-background: #0a0a0a;");
    chatListView.setPrefHeight(400);
    
    centerBox.getChildren().addAll(chatLabel, chatListView);
    VBox.setVgrow(chatListView, javafx.scene.layout.Priority.ALWAYS);
    root.setCenter(centerBox);

    VBox bottomBox = new VBox(5);
    bottomBox.setPadding(new Insets(10, 0, 0, 0));
    
    HBox inputBox = new HBox(10);
    inputBox.setAlignment(Pos.CENTER_LEFT);
    
    chatInputField = new TextField();
    chatInputField.setPromptText("Type your message and press Enter...");
    chatInputField.setStyle("-fx-background-color: #2a2a2a; -fx-text-fill: #cccccc; -fx-prompt-text-fill: #666666;");
    chatInputField.setOnAction(e -> sendChatMessage());
    HBox.setHgrow(chatInputField, javafx.scene.layout.Priority.ALWAYS);
    
    inputBox.getChildren().addAll(chatInputField);
    
    bottomBox.getChildren().addAll(inputBox);
    root.setBottom(bottomBox);

    Scene scene = new Scene(root, 500, 500);
    
    if (mainController != null) {
      String cssPath = getClass().getResource("/css/detective.css") != null 
          ? getClass().getResource("/css/detective.css").toExternalForm() 
          : null;
      if (cssPath != null) {
        scene.getStylesheets().add(cssPath);
      }
    }
    
    stage.setScene(scene);
  }

  private void sendChatMessage() {
    String message = chatInputField.getText().trim();
    
    if (message.isEmpty()) {
      return;
    }
    
    if (mainController != null) {
      mainController.sendCommand("/chat " + message);
    }
    
    chatInputField.clear();
  }

  public void addChatMessage(String sender, String message) {
    long timestampMillis = System.currentTimeMillis();
    ChatMessage chatMessage = new ChatMessage(sender, message, timestampMillis);
    addChatMessage(chatMessage);
  }

  public void addChatMessage(ChatMessage chatMessage) {
    String formattedMessage = formatMessage(chatMessage);
    chatListView.getItems().add(formattedMessage);
    scrollToBottom();

    if (!stage.isShowing() && mainController != null) {
      mainController.incrementUnreadChat();
    }
  }

  public void loadHistory(List<ChatMessage> history) {
    chatListView.getItems().clear();
    for (ChatMessage message : history) {
      chatListView.getItems().add(formatMessage(message));
    }
    scrollToBottom();
  }

  private String formatMessage(ChatMessage message) {
    LocalTime time = Instant.ofEpochMilli(message.getTimestamp()).atZone(ZoneId.systemDefault()).toLocalTime();
    String timestamp = time.format(TIME_FORMATTER);
    return "[" + timestamp + "] " + message.getSenderDisplayId() + ": " + message.getText();
  }

  private void scrollToBottom() {
    if (chatListView.getItems().size() > 0) {
      chatListView.scrollTo(chatListView.getItems().size() - 1);
    }
  }

  public void show() {
    if (stage != null) {
      stage.show();
      stage.toFront();
      chatInputField.requestFocus();
    }
  }

  public void hide() {
    if (stage != null) {
      stage.hide();
    }
  }

  public boolean isShowing() {
    return stage != null && stage.isShowing();
  }
}
