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
import javafx.stage.FileChooser;

import java.io.File;

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
        executeSyncTask(false);
    }

    // Fetch market prices over HTTP and show a detailed JSON request & parsing demonstration dialog
    @FXML
    public void handleSyncMarketDataWithDemo() {
        executeSyncTask(true);
    }

    // Internal helper to handle HTTP sync execution
    private void executeSyncTask(boolean showDetailedDemo) {
        if (hardwareSyncService == null) return;

        setStatusMessage("Dispatching live HTTP market sync to worker thread pool...");
        if (threadPoolStatusLabel != null) {
            threadPoolStatusLabel.setText("⚡ ThreadPool: Syncing HTTP/JSON...");
        }

        hardwareSyncService.syncAsynchronously(
                result -> {
                    onSyncComplete(result, showDetailedDemo);
                },
                error -> {
                    onSyncError(error);
                }
        );
    }

    // Open a FileChooser dialog to select and parse a manual local JSON file
    @FXML
    public void handleImportLocalJsonFile() {
        if (hardwareSyncService == null) return;

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Hardware Pricing JSON File");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("JSON Files (*.json)", "*.json")
        );

        // Pre-select data directory if present
        File dataDir = new File("data");
        if (dataDir.exists() && dataDir.isDirectory()) {
            fileChooser.setInitialDirectory(dataDir);
        }

        File selectedFile = fileChooser.showOpenDialog(
                rootBorderPane != null ? rootBorderPane.getScene().getWindow() : null
        );

        if (selectedFile == null) return;

        setStatusMessage("Parsing local JSON file [" + selectedFile.getName() + "]...");
        if (threadPoolStatusLabel != null) {
            threadPoolStatusLabel.setText("⚡ ThreadPool: Parsing Local JSON...");
        }

        hardwareSyncService.syncLocalFileAsynchronously(
                selectedFile,
                result -> {
                    onSyncComplete(result, true);
                },
                error -> {
                    onSyncError(error);
                }
        );
    }

    // Refresh UI components and show results dialog upon sync completion
    private void onSyncComplete(HardwareSyncService.SyncResult result, boolean showDetailedDemo) {
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

        if (showDetailedDemo) {
            showJsonSyncDemoDialog(result);
        } else {
            Alert info = new Alert(Alert.AlertType.INFORMATION);
            info.setTitle("Sync Complete");
            info.setHeaderText("Market Data Synchronized");
            info.setContentText(result.message() + "\nSource: " + result.source());
            info.showAndWait();
        }
    }

    // Show error alert on sync failure
    private void onSyncError(Throwable error) {
        setStatusMessage("Sync error: " + error.getMessage());
        if (threadPoolStatusLabel != null) {
            threadPoolStatusLabel.setText("⚡ ThreadPool: 4 Workers Ready");
        }
        Alert err = new Alert(Alert.AlertType.ERROR, "Sync failed: " + error.getMessage());
        err.showAndWait();
    }

    // Display a rich demonstration dialog showing HTTP Request info, raw JSON text, and Jackson parse details
    private void showJsonSyncDemoDialog(HardwareSyncService.SyncResult result) {
        Alert dialog = new Alert(Alert.AlertType.INFORMATION);
        dialog.setTitle("JSON Feature & HTTP Request Demonstration");
        dialog.setHeaderText("JSON Parsing & Market Data Sync Analysis");

        VBox container = new VBox(10);
        container.setPadding(new Insets(10));
        container.setPrefWidth(640);

        Label summaryLabel = new Label("Source: " + result.source() + " | Updated Components: " + result.updatedCount());
        summaryLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #1e293b;");

        Label logHeader = new Label("1. Protocol & Execution Details:");
        logHeader.setStyle("-fx-font-weight: bold; -fx-text-fill: #2563eb;");
        TextArea logArea = new TextArea(result.httpDetails());
        logArea.setEditable(false);
        logArea.setPrefRowCount(4);
        logArea.setStyle("-fx-font-family: monospace; -fx-font-size: 11px;");

        Label jsonHeader = new Label("2. Raw JSON Payload & Jackson Tree Node Parsing:");
        jsonHeader.setStyle("-fx-font-weight: bold; -fx-text-fill: #16a34a;");
        TextArea jsonArea = new TextArea(result.rawJson());
        jsonArea.setEditable(false);
        jsonArea.setPrefRowCount(10);
        jsonArea.setStyle("-fx-font-family: monospace; -fx-font-size: 11px;");

        container.getChildren().addAll(summaryLabel, logHeader, logArea, jsonHeader, jsonArea);
        dialog.getDialogPane().setContent(container);
        dialog.showAndWait();
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
