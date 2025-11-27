package ui.windows;

import javafx.scene.Scene;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class HelpWindow extends Stage {

    public HelpWindow() {
        setTitle("Help");

        // Create and style the layout
        VBox layout = new VBox();
        layout.setStyle("-fx-background-color: #2b2b2b; -fx-padding: 10;");

        // Create and style the text area
        TextArea helpText = new TextArea();
        helpText.setEditable(false);
        helpText.setText(getHelpContent());
        helpText.setStyle(
                "-fx-control-inner-background: #1a1a1a; " +
                "-fx-text-fill: #e0e0e0; " +
                "-fx-font-family: 'Courier New'; " +
                "-fx-font-size: 14px;"
        );
        helpText.setWrapText(true);

        layout.getChildren().add(helpText);
        VBox.setVgrow(helpText, javafx.scene.layout.Priority.ALWAYS);

        // Create and set the scene
        Scene scene = new Scene(layout, 500, 400);
        setScene(scene);
    }

    private String getHelpContent() {
        return "Available commands:\n" +
                "  look                       - View surroundings.\n" +
                "  move [direction]           - Move to another room.\n" +
                "  examine [object]           - Inspect an object.\n" +
                "  question [suspect]         - Question a suspect.\n" +
                "  deduce [object]            - Make a deduction about an object.\n" +
                "  journal                    - View your journal.\n" +
                "  journal add [note]         - Add a note to your journal.\n" +
                "  tasks                      - View case tasks.\n" +
                "  ask watson                 - Ask Dr. Watson for a hint.\n" +
                "  final exam                 - Initiate the final exam (if conditions met).\n" +
                "  exit                       - Exit the current case (MP) or game (SP).\n" +
                "  help                       - Display this help message.";
    }
}
