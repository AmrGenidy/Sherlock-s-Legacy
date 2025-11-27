package ui.windows;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import singleplayer.util.CaseFileUtil;
import ui.MainController;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

public class AddCaseWindow extends Stage {

    private final MainController mainController;
    private ListView<String> caseListView;

    public AddCaseWindow(MainController mainController) {
        this.mainController = mainController;

        initModality(Modality.APPLICATION_MODAL);
        setTitle("Add a New Case");

        VBox layout = new VBox(15);
        layout.setPadding(new Insets(20));
        layout.setStyle("-fx-background-color: #2b2b2b;");
        layout.setAlignment(Pos.CENTER);

        Label titleLabel = new Label("Manage Custom Cases");
        titleLabel.setStyle("-fx-font-size: 18; -fx-text-fill: #d4af37;");

        caseListView = new ListView<>();
        caseListView.setStyle("-fx-control-inner-background: #1a1a1a; -fx-text-fill: #00ff00;");
        refreshCaseList();

        Label pathLabel = new Label("Enter Full Path to Case .json File:");
        pathLabel.setStyle("-fx-text-fill: #cccccc;");

        TextField pathTextField = new TextField();
        pathTextField.setPromptText("e.g., C:\\Users\\YourUser\\Downloads\\new_case.json");

        Button addButton = new Button("Add Case");
        addButton.setOnAction(event -> {
            String path = pathTextField.getText();
            if (path != null && !path.trim().isEmpty()) {
                String result = CaseFileUtil.addCaseFile(path);
                showAlert(result);
                if (result.startsWith("Success")) {
                    refreshCaseList();
                }
                pathTextField.clear();
            } else {
                showAlert("Please enter a file path.");
            }
        });

        Button closeButton = new Button("Close");
        closeButton.setOnAction(event -> close());

        layout.getChildren().addAll(titleLabel, caseListView, pathLabel, pathTextField, addButton, closeButton);

        Scene scene = new Scene(layout, 500, 600);
        setScene(scene);
    }

    private void refreshCaseList() {
        List<String> caseFiles = CaseFileUtil.getAvailableCaseFiles().stream()
                .map(File::getName)
                .collect(Collectors.toList());
        caseListView.getItems().setAll(caseFiles);
    }

    private void showAlert(String message) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
            javafx.scene.control.Alert.AlertType.INFORMATION
        );
        alert.setTitle("Add Case Status");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
