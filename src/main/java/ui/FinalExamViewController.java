package ui;

import client.exam.FinalExamController;
import common.dto.ExamQuestionDTO;
import common.dto.FinalExamChoiceDTO;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import common.dto.ExamResultDTO;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;

import java.util.Map;

public class FinalExamViewController {

    @FXML
    private Label titleLabel;
    @FXML
    private Label progressLabel;
    @FXML
    private VBox questionPane;
    @FXML
    private Label questionPromptLabel;
    @FXML
    private ComboBox<FinalExamChoiceDTO> slot1ComboBox;
    @FXML
    private ComboBox<FinalExamChoiceDTO> slot2ComboBox;
    @FXML
    private Button previousButton;
    @FXML
    private Button nextButton;
    @FXML
    private Button submitButton;
    @FXML
    private ScrollPane resultScrollPane;
    @FXML
    private VBox resultVBox;

    // Add Retry Button programmatically or via FXML.
    // Since I can't edit FXML, I'll add it to resultVBox dynamically or check if I can modify FXML.
    // I'll add it dynamically in displayResults.

    private FinalExamController finalExamController;
    private boolean isHost;

    public void setFinalExamController(FinalExamController finalExamController, boolean isHost) {
        this.finalExamController = finalExamController;
        this.isHost = isHost;
        if (!isHost) {
            slot1ComboBox.setDisable(true);
            slot2ComboBox.setDisable(true);
            previousButton.setDisable(true);
            nextButton.setDisable(true);
            submitButton.setVisible(false);
            submitButton.setManaged(false);
        }
    }

    @FXML
    public void initialize() {
        // Previous button not used in strict linear flow
        previousButton.setVisible(false);
        previousButton.setManaged(false);

        // "Next" button logic can be merged with Submit, or used if we had navigation.
        // Requirements say "Confirm/Answer button... Immediately load next question".
        // So we should use submitButton or repurpose nextButton as the primary action.
        // Let's use nextButton as the "Answer" button for consistency if it's prominent.
        // Or submitButton. FXML usually has both? The file shows both.
        // I'll hide nextButton and use submitButton as the "Confirm Answer" button.
        nextButton.setVisible(false);
        nextButton.setManaged(false);

        submitButton.setText("Submit Answer");
        submitButton.setOnAction(event -> {
            // Disable button immediately to prevent double submissions
            submitButton.setDisable(true);

            // Gather answers
            java.util.Map<String, String> answers = new java.util.HashMap<>();
            if (slot1ComboBox.getValue() != null) answers.put("slot1", slot1ComboBox.getValue().getChoiceId());
            if (slot2ComboBox.getValue() != null) answers.put("slot2", slot2ComboBox.getValue().getChoiceId());

            // Only submit if all slots filled (basic validation)
            // Assuming 2 slots for now based on existing UI hardcoding
            if (answers.size() >= 2) {
                finalExamController.submitCurrentQuestion(answers);
            } else {
                // Re-enable if validation failed
                submitButton.setDisable(false);
            }
        });

        // We don't need listener to send updates on change anymore, only on submit.
    }

    public void displayResults(ExamResultDTO resultDTO) {
        Platform.runLater(() -> {
            // Hide the question pane and the submit button
            questionPane.setVisible(false);
            submitButton.setVisible(false);

            // Move resultScrollPane to center if it's not already (better layout)
            if (questionPane.getParent() instanceof javafx.scene.layout.BorderPane) {
                javafx.scene.layout.BorderPane borderPane = (javafx.scene.layout.BorderPane) questionPane.getParent();
                // Important: Remove it from its current position (right) before setting it to center
                // to avoid "duplicate children added" exception.
                borderPane.setRight(null);
                borderPane.setCenter(resultScrollPane);
            }

            resultScrollPane.setVisible(true);
            resultVBox.getChildren().clear();

            Label scoreLabel = new Label("Score: " + resultDTO.getScore() + " / " + resultDTO.getTotalQuestions());
            scoreLabel.setStyle("-fx-font-size: 18; -fx-font-weight: bold;");
            resultVBox.getChildren().add(scoreLabel);

            for (String detail : resultDTO.getReviewableAnswersInfo()) {
                Label detailLabel = new Label(detail);
                resultVBox.getChildren().add(detailLabel);
            }

            if (resultDTO.isCaseSolved()) {
                if (resultDTO.getWinningMessage() != null) {
                    Label winningLabel = new Label("\n" + resultDTO.getWinningMessage());
                    winningLabel.setStyle("-fx-font-size: 16; -fx-text-fill: green; -fx-wrap-text: true;");
                    resultVBox.getChildren().add(winningLabel);
                }
            }

            // If guest, show simple waiting message instead of controls
            if (!isHost) {
                Label waitingLabel = new Label("Waiting for host to continue...");
                waitingLabel.setStyle("-fx-font-size: 16; -fx-text-fill: #d4af37;");
                resultVBox.getChildren().add(waitingLabel);
            } else {
                // Host controls
                Button continueButton = new Button("Continue");
                continueButton.setOnAction(e -> {
                    if (finalExamController != null) {
                        finalExamController.exitExam();
                    }
                });

                Button mainMenuButton = new Button("Main Menu");
                mainMenuButton.setOnAction(e -> {
                    if (finalExamController != null) {
                        finalExamController.returnToMainMenu();
                    }
                });

                javafx.scene.layout.HBox buttonBox = new javafx.scene.layout.HBox(10, continueButton);
                // Only show the Main Menu button if the case is solved
                if (resultDTO.isCaseSolved()) {
                    buttonBox.getChildren().add(mainMenuButton);
                }
                buttonBox.setAlignment(javafx.geometry.Pos.CENTER);
                resultVBox.getChildren().add(buttonBox);
            }
        });
    }

    public void displayQuestion(ExamQuestionDTO questionDTO) {
        Platform.runLater(() -> {
            // Reset View State for new question (or retry)
            resetView();

            submitButton.setDisable(false); // Re-enable button for the new question
            progressLabel.setText("Question " + (questionDTO.getQuestionIndex() + 1) + " of " + questionDTO.getTotalQuestions());
            questionPromptLabel.setText(questionDTO.getQuestionPrompt());

            // Assuming two slots for now
            updateComboBox(slot1ComboBox, "slot1", questionDTO);
            updateComboBox(slot2ComboBox, "slot2", questionDTO);

            // Update submit button state or text if needed
            submitButton.setText("Submit Answer");
        });
    }

    private void resetView() {
        // Restore the question pane and hide results if they were showing
        // This is crucial for "Retry" or "Restart" scenarios
        if (resultScrollPane.getParent() instanceof javafx.scene.layout.BorderPane) {
             javafx.scene.layout.BorderPane borderPane = (javafx.scene.layout.BorderPane) resultScrollPane.getParent();
             borderPane.setCenter(null); // Clear center
             // We can't put questionPane back easily unless we kept a reference or it's managed?
             // Actually, questionPane is still in the scene graph but hidden?
             // NO, we removed it from parent if we did setCenter(resultScrollPane)!
             // Wait, earlier logic:
             // if (questionPane.getParent() instanceof BorderPane) { ... setCenter(resultScrollPane) }
             // This REPLACES questionPane in the center.
             // So questionPane is now orphaned (no parent).
             // We must put it back!
             borderPane.setCenter(questionPane);
             borderPane.setRight(resultScrollPane); // Put result back to right (hidden)
        }

        questionPane.setVisible(true);
        resultScrollPane.setVisible(false);
        submitButton.setVisible(true);
    }

    private void updateComboBox(ComboBox<FinalExamChoiceDTO> comboBox, String slotId, ExamQuestionDTO questionDTO) {
        comboBox.getItems().clear();
        comboBox.getItems().addAll(questionDTO.getSlots().get(slotId).getChoices());
        comboBox.setConverter(new javafx.util.StringConverter<>() {
            @Override
            public String toString(FinalExamChoiceDTO object) {
                return object == null ? null : object.getChoiceText();
            }

            @Override
            public FinalExamChoiceDTO fromString(String string) {
                return null;
            }
        });

        String selectedChoiceId = questionDTO.getSelectedAnswers().get(slotId);
        if (selectedChoiceId != null) {
            for (FinalExamChoiceDTO choice : comboBox.getItems()) {
                if (choice.getChoiceId().equals(selectedChoiceId)) {
                    comboBox.getSelectionModel().select(choice);
                    break;
                }
            }
        }
    }
}
