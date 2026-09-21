package com.pcanalyzer;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;


/**
 * Main application entry point for the PC Hardware Benchmark & Build Analyzer.
 *
 * Design Decision:
 * 1. JavaFX Lifecycle: Extends javafx.application.Application to manage UI lifecycle
 *    (init, start, stop).
 * 2. Clean Separation: Application class solely bootstraps the primary Stage,
 *    delegating UI assembly to FXML loaders and business logic to controllers/services.
 */
public class App extends Application {

    private static final String APP_TITLE = "PC Hardware Benchmark & Build Analyzer";
    private static final double MIN_WIDTH = 1080;
    private static final double MIN_HEIGHT = 720;

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

    public static void main(String[] args) {
        launch(args);
    }
}
