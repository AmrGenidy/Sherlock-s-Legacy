package ui.windows;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import ui.MainController;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Journal window for the Detective Game.
 * Allows players to search journal entries and add new notes.
 */
public class JournalWindow {

  private Stage stage;
  private MainController mainController;
  private ListView<String> entriesListView;
  private TextField searchField;
  private TextArea noteTextArea;
  private List<String> allEntries;

  public JournalWindow(MainController controller) {
    this.mainController = controller;
    this.allEntries = new ArrayList<>();
    initializeWindow();
  }

  private void initializeWindow() {
    stage = new Stage();
    stage.setTitle("Journal");

    BorderPane root = new BorderPane();
    root.setPadding(new Insets(10));
    root.setStyle("-fx-background-color: #1a1a1a;");

    VBox topBox = new VBox(5);
    Label searchLabel = new Label("Search Journal:");
    searchLabel.setStyle("-fx-text-fill: #d4af37; -fx-font-weight: bold;");
    
    searchField = new TextField();
    searchField.setPromptText("Enter keyword and press Enter...");
    searchField.setStyle("-fx-background-color: #2a2a2a; -fx-text-fill: #cccccc; -fx-prompt-text-fill: #666666;");
    searchField.setOnAction(e -> performSearch());
    
    topBox.getChildren().addAll(searchLabel, searchField);
    root.setTop(topBox);

    VBox centerBox = new VBox(5);
    centerBox.setPadding(new Insets(10, 0, 10, 0));
    
    Label entriesLabel = new Label("Journal Entries:");
    entriesLabel.setStyle("-fx-text-fill: #d4af37; -fx-font-weight: bold;");
    
    entriesListView = new ListView<>();
    entriesListView.setStyle("-fx-background-color: #0a0a0a; -fx-control-inner-background: #0a0a0a;");
    entriesListView.setPrefHeight(300);
    
    centerBox.getChildren().addAll(entriesLabel, entriesListView);
    VBox.setVgrow(entriesListView, javafx.scene.layout.Priority.ALWAYS);
    root.setCenter(centerBox);

    VBox bottomBox = new VBox(5);
    Label noteLabel = new Label("Add New Note:");
    noteLabel.setStyle("-fx-text-fill: #d4af37; -fx-font-weight: bold;");
    
    noteTextArea = new TextArea();
    noteTextArea.setPromptText("Type your note here...");
    noteTextArea.setPrefHeight(80);
    noteTextArea.setWrapText(true);
    noteTextArea.setStyle("-fx-control-inner-background: #0a0a0a; -fx-text-fill: #cccccc; -fx-prompt-text-fill: #666666;");
    noteTextArea.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
      if (e.getCode() == KeyCode.ENTER && !e.isShiftDown()) {
        e.consume(); // Consume the event to prevent a newline
        addNote();
      }
    });
    
    Button addNoteButton = new Button("Add Note");
    addNoteButton.setStyle("-fx-background-color: #3a3a3a; -fx-text-fill: #d4af37; -fx-font-weight: bold;");
    addNoteButton.setOnAction(e -> addNote());
    
    HBox buttonBox = new HBox();
    buttonBox.setAlignment(Pos.CENTER_RIGHT);
    buttonBox.getChildren().add(addNoteButton);
    
    bottomBox.getChildren().addAll(noteLabel, noteTextArea, buttonBox);
    root.setBottom(bottomBox);

    Scene scene = new Scene(root, 600, 500);
    
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

  private void performSearch() {
    String keyword = searchField.getText().trim().toLowerCase();
    if (keyword.isEmpty()) {
      updateEntriesList(allEntries);
      return;
    }

    List<String> filteredEntries = allEntries.stream()
            .filter(entry -> entry.toLowerCase().contains(keyword))
            .collect(Collectors.toList());

    updateEntriesList(filteredEntries);
  }

  private void addNote() {
    String note = noteTextArea.getText().trim();
    if (note.isEmpty()) {
      return;
    }
    
    if (mainController != null) {
      mainController.sendCommand("journal add " + note);
    }
    
    noteTextArea.clear();
  }

  private void updateEntriesList(List<String> entries) {
    entriesListView.getItems().clear();
    entriesListView.getItems().addAll(entries);
  }

  public void addEntry(String entry) {
    allEntries.add(entry);
    updateEntriesList(allEntries);
  }

  public void setEntries(List<common.dto.JournalEntryDTO> entries) {
    allEntries.clear();
    for (common.dto.JournalEntryDTO entry : entries) {
      allEntries.add(entry.toString());
    }
    updateEntriesList(allEntries);
  }

  public void show() {
    if (stage != null) {
      stage.show();
      stage.toFront();
    }
  }

  public void hide() {
    if (stage != null) {
      stage.hide();
    }
  }
}
