package com.pcanalyzer.ui;

import com.pcanalyzer.db.ComponentDao;
import com.pcanalyzer.db.DatabaseManager;
import com.pcanalyzer.db.SqliteComponentDao;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TabPane;

/**
 * Top-level coordinator controller for the main window.
 *
 * Design Decision:
 * 1. Thin Controller / Mediator Pattern: MainController coordinates communication
 *    between independent sub-views (BrowseView and BuildPanelView) without them needing
 *    direct coupling to each other.
 * 2. Dependency Injection: Instantiates and supplies the DAO layer to child controllers.
 */
public class MainController {

    @FXML private TabPane mainTabPane;
    @FXML private Label connectionStatusLabel;
    @FXML private Label statusMessageLabel;
    @FXML private Label componentCountLabel;

    // Injected nested controllers matching fx:id + "Controller"
    @FXML private BrowseController browseViewController;
    @FXML private BuildPanelController buildPanelViewController;

    private DatabaseManager dbManager;
    private ComponentDao componentDao;

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

        updateComponentCount();
        setStatusMessage("Application initialized. Local database ready.");
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

    @FXML
    private void handleSwitchToCatalog() {
        if (mainTabPane != null) {
            mainTabPane.getSelectionModel().select(0);
        }
    }
}
