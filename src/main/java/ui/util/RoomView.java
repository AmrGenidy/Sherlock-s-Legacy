package ui.util;

import common.dto.RoomDescriptionDTO;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TextInputDialog;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.animation.FadeTransition;
import javafx.util.Duration;
import ui.MainController;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Custom component for displaying room visualizations with clickable suspects and objects.
 */
public class RoomView extends StackPane {

  private MainController mainController;
  private ImageView roomBackgroundImage;
  private Pane interactiveLayer;
  private Map<String, ClickableElement> suspects;
  private Map<String, ClickableElement> objects;
  private Label roomNameLabel;

  public RoomView(MainController controller) {
    this.mainController = controller;
    this.suspects = new HashMap<>();
    this.objects = new HashMap<>();
    initializeView();
  }

  private void initializeView() {
    // Background image layer
    roomBackgroundImage = new ImageView();
    roomBackgroundImage.setPreserveRatio(true);
    roomBackgroundImage.fitWidthProperty().bind(this.widthProperty());
    roomBackgroundImage.fitHeightProperty().bind(this.heightProperty());

    // Interactive layer for suspects and objects
    interactiveLayer = new Pane();
    interactiveLayer.setStyle("-fx-background-color: transparent;");

    // Room name label
    roomNameLabel = new Label("Room Name");
    roomNameLabel.setStyle("-fx-background-color: rgba(42, 42, 42, 0.8); -fx-text-fill: #d4af37; " +
                            "-fx-font-weight: bold; -fx-font-size: 18; -fx-padding: 10 20 10 20; " +
                            "-fx-background-radius: 5;");
    StackPane.setAlignment(roomNameLabel, Pos.TOP_CENTER);

    // Add layers to the stack
    this.getChildren().addAll(roomBackgroundImage, interactiveLayer, roomNameLabel);
  }

  /**
   * Loads and displays a room based on RoomDescriptionDTO.
   */
  public void loadRoom(RoomDescriptionDTO roomDescription) {
    // Clear previous room elements
    clear();

    // Set room name
    roomNameLabel.setText(roomDescription.getName());

    // Load room background image
    Image roomImage = ImageResourceLoader.loadRoomImage(roomDescription.getName());
    if (roomImage != null) {
      roomBackgroundImage.setImage(roomImage);
    } else {
      createPlaceholderBackground(roomDescription.getName());
    }

    // Add suspects
    for (int i = 0; i < roomDescription.getOccupantNames().size(); i++) {
      String suspectName = roomDescription.getOccupantNames().get(i);
      // Position suspects horizontally across the room
      double xPos = 0.2 + (i * 0.3);
      double yPos = 0.5;
      addSuspect(suspectName, xPos, yPos);
    }

    // Add objects
    for (int i = 0; i < roomDescription.getObjectNames().size(); i++) {
      String objectName = roomDescription.getObjectNames().get(i);
      // Position objects at different locations
      double xPos = 0.3 + (i * 0.2);
      double yPos = 0.7;
      addObject(objectName, xPos, yPos);
    }
  }

  /**
   * Creates a placeholder background when image is not available.
   */
  private void createPlaceholderBackground(String roomName) {
    // Create a simple colored background
    this.setStyle("-fx-background-color: linear-gradient(to bottom, #2a2a2a, #1a1a1a);");
  }

  /**
   * Adds a clickable suspect to the room.
   */
  private void addSuspect(String suspectName, double xPos, double yPos) {
    ClickableElement element = createClickableElement(suspectName, xPos, yPos, true);
    suspects.put(suspectName, element);
    interactiveLayer.getChildren().add(element);
  }

  /**
   * Adds a clickable object to the room.
   */
  private void addObject(String objectName, double xPos, double yPos) {
    ClickableElement element = createClickableElement(objectName, xPos, yPos, false);
    objects.put(objectName, element);
    interactiveLayer.getChildren().add(element);
  }

  /**
   * Creates a clickable element (suspect or object).
   */
  private ClickableElement createClickableElement(String name, double xPos, double yPos, boolean isSuspect) {
    ClickableElement element = new ClickableElement(name, isSuspect);

    // Position element (will be bound to actual position after layout)
    element.setTranslateX(xPos * 800 - 40); // Approximate positioning
    element.setTranslateY(yPos * 600 - 40);

    // Set click handler, unless it's a player character
    if (!name.startsWith("Player-")) {
      element.setOnMouseClicked(e -> {
        if (isSuspect) {
          showSuspectDialog(name);
        } else {
          showObjectDialog(name);
        }
      });
    }

    return element;
  }

  /**
   * Shows a dialog for interacting with a suspect.
   */
  private void showSuspectDialog(String suspectName) {
    if ("Dr. Watson".equals(suspectName)) {
      showAskWatsonDialog();
    } else {
      showGenericSuspectDialog(suspectName);
    }
  }

  /**
   * Shows a dialog for asking Dr. Watson for a hint.
   */
  private void showAskWatsonDialog() {
    Alert dialog = new Alert(Alert.AlertType.NONE);
    dialog.setTitle("Ask Dr. Watson");
    dialog.setHeaderText("You can ask Dr. Watson for a hint.");

    ButtonType askWatsonButton = new ButtonType("Ask Watson");
    ButtonType cancelButton = new ButtonType("Cancel");

    dialog.getButtonTypes().setAll(askWatsonButton, cancelButton);

    // Apply custom styling
    dialog.getDialogPane().setStyle(
            "-fx-background-color: #2b2b2b; " +
            "-fx-border-color: #d4af37; " +
            "-fx-border-width: 1;"
    );
    dialog.getDialogPane().lookup(".content.label").setStyle("-fx-text-fill: #e0e0e0;");
    dialog.getDialogPane().lookup(".header-panel").setStyle("-fx-background-color: #1a1a1a;");
    dialog.getDialogPane().lookup(".header-panel .label").setStyle("-fx-text-fill: #d4af37; -fx-font-weight: bold;");

    Optional<ButtonType> result = dialog.showAndWait();

    if (result.isPresent() && result.get() == askWatsonButton) {
      mainController.sendCommand("ask watson");
      showSpeechBubble(suspects.get("Dr. Watson"), "Asking Dr. Watson...");
    }
  }

  /**
   * Shows a generic dialog for interacting with any suspect other than Dr. Watson.
   */
  private void showGenericSuspectDialog(String suspectName) {
    Alert dialog = new Alert(Alert.AlertType.NONE);
    dialog.setTitle("Interact with " + suspectName);
    dialog.setHeaderText(suspectName);
    dialog.setContentText("How would you like to interact with this suspect?");

    ButtonType questionButton = new ButtonType("Question");
    ButtonType deduceButton = new ButtonType("Deduce");
    ButtonType cancelButton = new ButtonType("Cancel");

    dialog.getButtonTypes().setAll(questionButton, deduceButton, cancelButton);

    // Apply custom styling
    dialog.getDialogPane().setStyle(
            "-fx-background-color: #2b2b2b; " +
            "-fx-border-color: #d4af37; " +
            "-fx-border-width: 1;"
    );
    dialog.getDialogPane().lookup(".content.label").setStyle("-fx-text-fill: #e0e0e0;");
    dialog.getDialogPane().lookup(".header-panel").setStyle("-fx-background-color: #1a1a1a;");
    dialog.getDialogPane().lookup(".header-panel .label").setStyle("-fx-text-fill: #d4af37; -fx-font-weight: bold;");

    Optional<ButtonType> result = dialog.showAndWait();

    if (result.isPresent()) {
      if (result.get() == questionButton) {
        mainController.sendCommand("question " + suspectName);
        showSpeechBubble(suspects.get(suspectName), "Questioning " + suspectName + "...");
      } else if (result.get() == deduceButton) {
        mainController.sendCommand("deduce " + suspectName);
        showSpeechBubble(suspects.get(suspectName), "Analyzing " + suspectName + "...");
      }
    }
  }

  /**
   * Shows a dialog for interacting with an object.
   */
  private void showObjectDialog(String objectName) {
    Alert dialog = new Alert(Alert.AlertType.NONE);
    dialog.setTitle("Interact with " + objectName);
    dialog.setHeaderText(objectName);
    dialog.setContentText("How would you like to interact with this object?");

    ButtonType examineButton = new ButtonType("Examine");
    ButtonType deduceButton = new ButtonType("Deduce");
    ButtonType cancelButton = new ButtonType("Cancel");

    dialog.getButtonTypes().setAll(examineButton, deduceButton, cancelButton);

    // Apply custom styling
    dialog.getDialogPane().setStyle(
            "-fx-background-color: #2b2b2b; " +
            "-fx-border-color: #d4af37; " +
            "-fx-border-width: 1;"
    );
    dialog.getDialogPane().lookup(".content.label").setStyle("-fx-text-fill: #e0e0e0;");
    dialog.getDialogPane().lookup(".header-panel").setStyle("-fx-background-color: #1a1a1a;");
    dialog.getDialogPane().lookup(".header-panel .label").setStyle("-fx-text-fill: #d4af37; -fx-font-weight: bold;");

    Optional<ButtonType> result = dialog.showAndWait();
    
    if (result.isPresent()) {
      if (result.get() == examineButton) {
        mainController.sendCommand("examine " + objectName);
        showSpeechBubble(objects.get(objectName), "Examining " + objectName + "...");
      } else if (result.get() == deduceButton) {
        mainController.sendCommand("deduce " + objectName);
        showSpeechBubble(objects.get(objectName), "Deducing from " + objectName + "...");
      }
    }
  }

  /**
   * Shows a speech bubble above an element with a message.
   */
  private void showSpeechBubble(ClickableElement element, String message) {
    if (element == null) return;

    Label bubble = new Label(message);
    bubble.setStyle("-fx-background-color: rgba(255, 255, 255, 0.95); -fx-text-fill: #000000; " +
                    "-fx-padding: 10; -fx-background-radius: 10; -fx-font-size: 12; " +
                    "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.5), 5, 0, 0, 2);");
    bubble.setWrapText(true);
    bubble.setMaxWidth(200);

    // Position bubble above the element
    bubble.setTranslateX(element.getTranslateX() - 60);
    bubble.setTranslateY(element.getTranslateY() - 80);

    interactiveLayer.getChildren().add(bubble);

    // Fade out and remove after 3 seconds
    FadeTransition fadeOut = new FadeTransition(Duration.seconds(1), bubble);
    fadeOut.setDelay(Duration.seconds(2));
    fadeOut.setFromValue(1.0);
    fadeOut.setToValue(0.0);
    fadeOut.setOnFinished(e -> interactiveLayer.getChildren().remove(bubble));
    fadeOut.play();
  }

  /**
   * Shows a speech bubble with a response message (called from external updates).
   */
  public void showResponseBubble(String targetName, String response) {
    ClickableElement element = suspects.get(targetName);
    if (element == null) {
      element = objects.get(targetName);
    }
    
    if (element != null) {
      showSpeechBubble(element, response);
    }
  }

  /**
   * Clears all room elements and resets the view to a default state.
   */
  public void clear() {
    suspects.clear();
    objects.clear();
    interactiveLayer.getChildren().clear();
    roomBackgroundImage.setImage(null);
    roomNameLabel.setText("Room Name");
    // Reset any placeholder styling
    this.setStyle("-fx-background-color: #1a1a1a;");
  }

  /**
   * Gets the image path for a room.
   */
  private String getRoomImagePath(String roomName) {
    // Convert room name to lowercase and replace spaces with underscores
    String imageName = roomName.toLowerCase().replace(" ", "_") + ".png";
    return "/images/rooms/" + imageName;
  }

  /**
   * Inner class representing a clickable element (suspect or object).
   */
  private static class ClickableElement extends StackPane {
    private String name;
    private boolean isSuspect;

    public ClickableElement(String name, boolean isSuspect) {
      this.name = name;
      this.isSuspect = isSuspect;
      createVisual();
    }

    private void createVisual() {
      // Create a circular icon with label
      Circle circle = new Circle(40);
      circle.setFill(isSuspect ? Color.web("#4a7ba7") : Color.web("#8b7355"));
      circle.setStroke(Color.web("#d4af37"));
      circle.setStrokeWidth(3);

      Label label = new Label(name.length() > 15 ? name.substring(0, 15) + "..." : name);
      label.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 10;");
      label.setWrapText(true);
      label.setMaxWidth(70);
      label.setAlignment(Pos.CENTER);

      this.getChildren().addAll(circle, label);

      // Make non-player characters interactive
      if (!name.startsWith("Player-")) {
        this.setStyle("-fx-cursor: hand;");

        // Hover effect
        this.setOnMouseEntered(e -> {
          circle.setScaleX(1.1);
          circle.setScaleY(1.1);
          circle.setStrokeWidth(4);
        });

        this.setOnMouseExited(e -> {
          circle.setScaleX(1.0);
          circle.setScaleY(1.0);
          circle.setStrokeWidth(3);
        });
      }
    }

    public String getName() {
      return name;
    }
  }
}
