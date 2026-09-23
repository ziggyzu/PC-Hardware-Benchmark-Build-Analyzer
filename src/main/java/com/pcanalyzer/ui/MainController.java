package com.pcanalyzer.ui;

import com.pcanalyzer.db.ComponentDao;
import com.pcanalyzer.db.DatabaseManager;
import com.pcanalyzer.db.SqliteComponentDao;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;

/**
 * Top-level coordinator controller for the main application window.
 *
 * Demonstrates:
 * 1. Rich JavaFX UI Design: MenuBar, Menu, MenuItem, PasswordField in security dialog, TabPane, Labels.
 * 2. Layout Responsiveness: Dynamically coordinates sub-views and handles window layout.
 * 3. Thin Controller / Mediator Pattern: Coordinates inter-view communication without coupling child views.
 */
public class MainController {

    @FXML private BorderPane rootBorderPane;
    @FXML private TabPane mainTabPane;
    @FXML private Label adminStatusLabel;
    @FXML private Label connectionStatusLabel;
    @FXML private Label statusMessageLabel;
    @FXML private Label componentCountLabel;

    // Injected nested controllers matching fx:id + "Controller"
    @FXML private BrowseController browseViewController;
    @FXML private BuildPanelController buildPanelViewController;
    @FXML private ComparisonController comparisonViewController;

    private DatabaseManager dbManager;
    private ComponentDao componentDao;
    private boolean isAdmin = false;

    @FXML
    public void initialize() {
        // Initialize SQLite Database
        dbManager = new DatabaseManager();
        dbManager.initialize();
        componentDao = new SqliteComponentDao(dbManager);

        // Wire dependencies to child controllers
        if (browseViewController != null) {
            browseViewController.setComponentDao(componentDao);

            // Wire "Add to Build" event from catalog to build panel
            browseViewController.setOnAddToBuildCallback(component -> {
                if (buildPanelViewController != null) {
                    buildPanelViewController.addComponentToBuild(component);
                    setStatusMessage("Added " + component.getName() + " to current build.");
                }
            });

            // Wire status messaging
            browseViewController.setOnStatusMessageCallback(this::setStatusMessage);
        }

        // Wire dependencies to Comparison tab controller
        if (comparisonViewController != null) {
            comparisonViewController.setComponentDao(componentDao);
        }

        updateComponentCount();
        setStatusMessage("Application initialized. SQLite local database ready.");
    }

    public void setStatusMessage(String message) {
        Platform.runLater(() -> {
            if (statusMessageLabel != null) {
                statusMessageLabel.setText(message);
            }
            updateComponentCount();
        });
    }

    private void updateComponentCount() {
        if (componentDao != null && componentCountLabel != null) {
            int count = componentDao.findAll().size();
            componentCountLabel.setText("Catalog: " + count + " items");
        }
    }

    // =========================================================================
    // Menu Actions & Security Dialog (Featuring PasswordField)
    // =========================================================================

    @FXML
    public void handleAdminUnlockDialog() {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Admin Security Authentication");
        dialog.setHeaderText("Enter Admin Password to unlock elevated privileges");

        Label label = new Label("Security Password:");
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Default: admin123");

        VBox content = new VBox(8, label, passwordField);
        content.setPadding(new Insets(16));
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == ButtonType.OK) {
                return passwordField.getText();
            }
            return null;
        });

        dialog.showAndWait().ifPresent(password -> {
            if ("admin123".equals(password)) {
                isAdmin = true;
                adminStatusLabel.setText("[Admin Mode: Active]");
                adminStatusLabel.setStyle("-fx-text-fill: #16a34a; -fx-font-weight: bold;");
                setStatusMessage("Admin authorization granted. Advanced permissions unlocked.");
            } else {
                setStatusMessage("Admin authorization failed: Invalid passkey.");
                Alert alert = new Alert(Alert.AlertType.ERROR, "Authentication failed. Incorrect password.");
                alert.showAndWait();
            }
        });
    }

    @FXML
    public void handleRefreshDatabase() {
        if (browseViewController != null) {
            browseViewController.loadComponentsFromDatabase();
        }
        setStatusMessage("Database refreshed.");
    }

    @FXML
    public void handleExit() {
        Platform.exit();
        System.exit(0);
    }

    @FXML
    public void handleSwitchToCatalog() {
        if (mainTabPane != null) {
            mainTabPane.getSelectionModel().select(0);
        }
    }

    @FXML
    public void handleSwitchToBuild() {
        if (mainTabPane != null) {
            mainTabPane.getSelectionModel().select(1);
        }
    }

    @FXML
    public void handleSwitchToComparison() {
        if (mainTabPane != null) {
            mainTabPane.getSelectionModel().select(2);
        }
    }

    @FXML
    public void handleAboutDialog() {
        Alert about = new Alert(Alert.AlertType.INFORMATION);
        about.setTitle("About PC Hardware Analyzer");
        about.setHeaderText("PC Hardware Benchmark & Build Analyzer v1.0");
        about.setContentText("A JavaFX desktop application utilizing SQLite JDBC, reactive property bindings, " +
                "and multi-dimensional component analysis.");
        about.showAndWait();
    }
}
