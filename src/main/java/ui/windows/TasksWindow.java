package ui.windows;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * Tasks window for the Detective Game.
 * Displays case tasks and recommendations, allowing players to mark them as completed.
 */
public class TasksWindow {

  private Stage stage;
  private VBox tasksContainer;
  private ui.MainController mainController;
  private java.util.List<String> currentTasks;

  public TasksWindow(ui.MainController mainController) {
    this.mainController = mainController;
    this.currentTasks = new java.util.ArrayList<>();
    initializeWindow();
  }

  private void initializeWindow() {
    stage = new Stage();
    stage.setTitle("Tasks");

    // Main layout
    BorderPane root = new BorderPane();
    root.setPadding(new Insets(10));
    root.setStyle("-fx-background-color: #1a1a1a;");

    // Top: Title
    Label titleLabel = new Label("Case Tasks & Recommendations");
    titleLabel.setStyle("-fx-text-fill: #d4af37; -fx-font-weight: bold; -fx-font-size: 16;");
    titleLabel.setPadding(new Insets(0, 0, 10, 0));
    root.setTop(titleLabel);

    // Center: Tasks list in a ScrollPane
    ScrollPane scrollPane = new ScrollPane();
    scrollPane.setFitToWidth(true);
    scrollPane.setStyle("-fx-background-color: #0a0a0a; -fx-background: #0a0a0a;");
    
    tasksContainer = new VBox(10);
    tasksContainer.setPadding(new Insets(10));
    tasksContainer.setStyle("-fx-background-color: #0a0a0a;");
    
    scrollPane.setContent(tasksContainer);
    root.setCenter(scrollPane);

    // Bottom: Instructions
    Label instructionsLabel = new Label("Tip: Check off tasks as you complete them to track your progress.");
    instructionsLabel.setStyle("-fx-text-fill: #999999; -fx-font-size: 11; -fx-font-style: italic;");
    instructionsLabel.setWrapText(true);
    instructionsLabel.setPadding(new Insets(10, 0, 0, 0));
    root.setBottom(instructionsLabel);

    // Create scene
    Scene scene = new Scene(root, 500, 400);
    
    // Apply CSS if available
    String cssPath = getClass().getResource("/css/detective.css") != null 
        ? getClass().getResource("/css/detective.css").toExternalForm() 
        : null;
    if (cssPath != null) {
      scene.getStylesheets().add(cssPath);
    }
    
    stage.setScene(scene);
  }

  /**
   * Adds a task to the tasks list.
   */
  private void addTask(String taskDescription, boolean completed) {
    HBox taskBox = new HBox(10);
    taskBox.setAlignment(Pos.CENTER_LEFT);
    taskBox.setPadding(new Insets(5));

    CheckBox checkBox = new CheckBox();
    checkBox.setSelected(completed);
    checkBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
      updateTaskStyle(taskBox, newVal);
      if (mainController != null) {
        mainController.updateTaskState(taskDescription, newVal);
      }
    });

    Label taskLabel = new Label(taskDescription);
    taskLabel.setWrapText(true);
    taskLabel.setMaxWidth(Double.MAX_VALUE);
    HBox.setHgrow(taskLabel, javafx.scene.layout.Priority.ALWAYS);

    taskBox.getChildren().addAll(checkBox, taskLabel);
    updateTaskStyle(taskBox, completed);

    tasksContainer.getChildren().add(taskBox);
  }

  /**
   * Updates the visual style of a task based on completion status.
   */
  private void updateTaskStyle(HBox taskBox, boolean completed) {
    if (completed) {
      taskBox.setStyle("-fx-background-color: #2a3a2a; -fx-background-radius: 5;");
      // Add strikethrough to label
      Label label = (Label) taskBox.getChildren().get(1);
      label.setStyle("-fx-text-fill: #888888; -fx-font-size: 13; -fx-strikethrough: true;");
    } else {
      taskBox.setStyle("-fx-background-color: #1a1a1a; -fx-background-radius: 5;");
      Label label = (Label) taskBox.getChildren().get(1);
      label.setStyle("-fx-text-fill: #cccccc; -fx-font-size: 13;");
    }
  }

  /**
   * Clears all tasks.
   */
  public void clearTasks() {
    tasksContainer.getChildren().clear();
  }

  /**
   * Loads tasks from a list of strings.
   */
  public void loadTasks(java.util.List<String> tasks, java.util.Map<String, Boolean> taskStates) {
    clearTasks();
    this.currentTasks.clear();

    if (tasks != null) {
      for (String task : tasks) {
        boolean isCompleted = taskStates.getOrDefault(task, false);
        addTask(task, isCompleted);
      }
      this.currentTasks.addAll(tasks);
    }
  }

  /**
   * Shows the tasks window.
   */
  public void show() {
    if (stage != null) {
      stage.show();
      stage.toFront();
    }
  }

  /**
   * Hides the tasks window.
   */
  public void hide() {
    if (stage != null) {
      stage.hide();
    }
  }

  public boolean isShowing() {
    return stage != null && stage.isShowing();
  }
}
