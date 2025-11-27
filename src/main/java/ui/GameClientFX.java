package ui;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * JavaFX launcher for the Detective Game GUI client.
 * This class initializes the JavaFX application, loads the FXML, and passes
 * control to the MainController.
 */
public class GameClientFX extends Application {

  public static void main(String[] args) {
    launch(args);
  }

  @Override
  public void start(Stage primaryStage) throws Exception {
    // Load FXML
    FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main.fxml"));
    Parent root = loader.load();
    MainController mainController = loader.getController();

    // Pass command-line arguments to the controller
    mainController.setHostServices(getHostServices());
    mainController.setLaunchArgs(getParameters().getRaw());

    // Set up scene
    Scene scene = new Scene(root, 1280, 800);
    
    // Load CSS if available
    String cssPath = getClass().getResource("/css/theme_dark.css") != null
        ? getClass().getResource("/css/theme_dark.css").toExternalForm()
        : null;
    if (cssPath != null) {
      scene.getStylesheets().add(cssPath);
    }

    // Set up stage
    primaryStage.setTitle("Detective Game");
    primaryStage.setScene(scene);

    // The MainController will now handle the shutdown logic.
    primaryStage.setOnCloseRequest(event -> {
      mainController.shutdown();
      event.consume(); // Prevent the window from closing immediately
    });

    // Show the stage
    primaryStage.show();
    
    System.out.println("========================================");
    System.out.println("  Detective Game JavaFX Client Started");
    System.out.println("========================================");
  }

  @Override
  public void stop() throws Exception {
    // Shutdown is now handled by the MainController's onCloseRequest logic.
    // This method is kept for lifecycle completeness but doesn't need to do anything.
    super.stop();
  }
}
