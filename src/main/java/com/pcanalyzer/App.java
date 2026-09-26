package com.pcanalyzer;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

// Main entry point that starts the desktop app
public class App extends Application {

    private static final String APP_TITLE = "PC Hardware Benchmark & Build Analyzer";
    private static final double MIN_WIDTH = 1080;
    private static final double MIN_HEIGHT = 720;

    // Load the layout file and display the main application window
    @Override
    public void start(Stage primaryStage) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/pcanalyzer/view/main-view.fxml"));
        Parent root = loader.load();

        Scene scene = new Scene(root, 1180, 760);
        scene.getStylesheets().add(getClass().getResource("/com/pcanalyzer/css/styles.css").toExternalForm());

        primaryStage.setTitle(APP_TITLE);
        primaryStage.setMinWidth(MIN_WIDTH);
        primaryStage.setMinHeight(MIN_HEIGHT);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    // Shut down background worker threads when closing the app
    @Override
    public void stop() throws Exception {
        com.pcanalyzer.util.ThreadPoolManager.getInstance().shutdown();
        super.stop();
    }

    // Launch the application
    public static void main(String[] args) {
        launch(args);
    }
}
