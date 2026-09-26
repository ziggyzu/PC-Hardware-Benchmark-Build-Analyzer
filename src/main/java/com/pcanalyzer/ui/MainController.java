package com.pcanalyzer.ui;

import com.pcanalyzer.db.ComponentDao;
import com.pcanalyzer.db.DatabaseManager;
import com.pcanalyzer.db.PriceHistoryDao;
import com.pcanalyzer.db.SqliteComponentDao;
import com.pcanalyzer.db.SqlitePriceHistoryDao;
import com.pcanalyzer.service.HardwareSyncService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;

// Main controller that connects the header, tabs, and status bar together
public class MainController {

    // Main window controls
    @FXML private BorderPane rootBorderPane;
    @FXML private TabPane mainTabPane;
    @FXML private Label adminStatusLabel;
    @FXML private Label connectionStatusLabel;
    @FXML private Label statusMessageLabel;
    @FXML private Label threadPoolStatusLabel;
    @FXML private Label componentCountLabel;

    // Sub-controllers for each tab
    @FXML private BrowseController browseViewController;
    @FXML private BuildPanelController buildPanelViewController;
    @FXML private ComparisonController comparisonViewController;

    // Database and background service helpers
    private DatabaseManager dbManager;
    private ComponentDao componentDao;
    private PriceHistoryDao priceHistoryDao;
    private HardwareSyncService hardwareSyncService;
    private boolean isAdmin = false;

    // Connect to the database and get all views ready
    @FXML
    public void initialize() {
        // Start up the SQLite database and create tables if missing
        dbManager = new DatabaseManager();
        dbManager.initialize();
        componentDao = new SqliteComponentDao(dbManager);
        priceHistoryDao = new SqlitePriceHistoryDao(dbManager);
        hardwareSyncService = new HardwareSyncService(componentDao, priceHistoryDao);

        // Set up the catalog tab and connect it to the build tab
        if (browseViewController != null) {
            browseViewController.setComponentDao(componentDao);

            // When a user clicks 'Add to Build', send the part to the build tab
            browseViewController.setOnAddToBuildCallback(component -> {
                if (buildPanelViewController != null) {
                    buildPanelViewController.addComponentToBuild(component);
                    setStatusMessage("Added " + component.getName() + " to current build.");
                }
            });

            // Forward status messages to the bottom bar
            browseViewController.setOnStatusMessageCallback(this::setStatusMessage);
        }

        // Pass the database connection to the comparison tab
        if (comparisonViewController != null) {
            comparisonViewController.setComponentDao(componentDao);
        }

        // Show initial component count and ready status
        updateComponentCount();
        setStatusMessage("Application initialized. SQLite database & ThreadPool ready.");
    }

    // Update the message in the bottom status bar safely from any thread
    public void setStatusMessage(String message) {
        Platform.runLater(() -> {
            if (statusMessageLabel != null) {
                statusMessageLabel.setText(message);
            }
            updateComponentCount();
        });
    }

    // Recalculate how many parts are saved in the catalog
    private void updateComponentCount() {
        if (componentDao != null && componentCountLabel != null) {
            int count = componentDao.findAll().size();
            componentCountLabel.setText("Catalog: " + count + " items");
        }
    }

    // Fetch the latest market prices in the background without freezing the window
    @FXML
    public void handleSyncMarketData() {
        if (hardwareSyncService == null) return;

        // Show that the sync is running
        setStatusMessage("Dispatching live market sync to worker thread pool...");
        if (threadPoolStatusLabel != null) {
            threadPoolStatusLabel.setText("⚡ ThreadPool: Syncing HTTP/JSON...");
        }

        // Run the background sync job
        hardwareSyncService.syncAsynchronously(
                result -> {
                    // Update the status and refresh the screens once finished
                    setStatusMessage(result.message());
                    if (threadPoolStatusLabel != null) {
                        threadPoolStatusLabel.setText("⚡ ThreadPool: 4 Workers Ready");
                    }
                    if (browseViewController != null) {
                        browseViewController.loadComponentsFromDatabase();
                    }
                    if (comparisonViewController != null) {
                        comparisonViewController.handleRefresh();
                    }

                    // Alert the user that the sync succeeded
                    Alert info = new Alert(Alert.AlertType.INFORMATION);
                    info.setTitle("Sync Complete");
                    info.setHeaderText("Market Data Synchronized");
                    info.setContentText(result.message() + "\nSource: " + result.source());
                    info.showAndWait();
                },
                error -> {
                    // Alert the user if the sync failed
                    setStatusMessage("Sync error: " + error.getMessage());
                    if (threadPoolStatusLabel != null) {
                        threadPoolStatusLabel.setText("⚡ ThreadPool: 4 Workers Ready");
                    }
                    Alert err = new Alert(Alert.AlertType.ERROR, "Sync failed: " + error.getMessage());
                    err.showAndWait();
                }
        );
    }

    // Check if the user's password is correct to unlock admin mode
    @FXML
    public void handleAdminUnlockDialog() {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Admin Security Authentication");
        dialog.setHeaderText("Enter Admin Password to unlock elevated privileges");

        // Ask for the admin password
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

        // Verify the entered password
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

    // Reload parts from the database
    @FXML
    public void handleRefreshDatabase() {
        if (browseViewController != null) {
            browseViewController.loadComponentsFromDatabase();
        }
        setStatusMessage("Database refreshed.");
    }

    // Close the program
    @FXML
    public void handleExit() {
        Platform.exit();
        System.exit(0);
    }

    // Switch to the hardware catalog tab
    @FXML
    public void handleSwitchToCatalog() {
        if (mainTabPane != null) {
            mainTabPane.getSelectionModel().select(0);
        }
    }

    // Switch to the build builder tab
    @FXML
    public void handleSwitchToBuild() {
        if (mainTabPane != null) {
            mainTabPane.getSelectionModel().select(1);
        }
    }

    // Switch to the performance charts tab
    @FXML
    public void handleSwitchToComparison() {
        if (mainTabPane != null) {
            mainTabPane.getSelectionModel().select(2);
        }
    }

    // Show a popup with general information about the app
    @FXML
    public void handleAboutDialog() {
        Alert about = new Alert(Alert.AlertType.INFORMATION);
        about.setTitle("About PC Hardware Analyzer");
        about.setHeaderText("PC Hardware Benchmark & Build Analyzer v1.0");
        about.setContentText("A JavaFX desktop application utilizing SQLite JDBC, reactive property bindings, " +
                "concurrency with Thread Pools, and multi-dimensional component analysis.");
        about.showAndWait();
    }
}
