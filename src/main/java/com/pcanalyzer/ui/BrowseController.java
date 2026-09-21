package com.pcanalyzer.ui;

import com.pcanalyzer.db.ComponentDao;
import com.pcanalyzer.model.Component;
import com.pcanalyzer.model.ComponentType;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;

/**
 * Controller for the Hardware Catalog browse and search view.
 *
 * Design Decision:
 * 1. Thin Controller: Coordinates TableView events, delegating DB queries to ComponentDao
 *    and build additions to a Consumer callback (MainController).
 * 2. High-Performance In-Memory Filtering: Uses JavaFX FilteredList and SortedList so typing
 *    in the search box provides instant 60fps filtering without issuing repeated database queries.
 */
public class BrowseController {

    @FXML private ComboBox<String> typeFilterComboBox;
    @FXML private TextField searchTextField;
    @FXML private TableView<Component> componentTableView;
    @FXML private TableColumn<Component, Number> idColumn;
    @FXML private TableColumn<Component, String> typeColumn;
    @FXML private TableColumn<Component, String> brandColumn;
    @FXML private TableColumn<Component, String> nameColumn;
    @FXML private TableColumn<Component, Number> priceColumn;
    @FXML private TableColumn<Component, Number> tdpColumn;
    @FXML private TableColumn<Component, String> specsColumn;

    @FXML private Button addToBuildButton;
    @FXML private Button deleteButton;
    @FXML private Label tableSummaryLabel;

    private ComponentDao componentDao;
    private final ObservableList<Component> masterData = FXCollections.observableArrayList();
    private FilteredList<Component> filteredData;
    private Consumer<Component> onAddToBuildCallback;
    private Consumer<String> onStatusMessageCallback;

    @FXML
    public void initialize() {
        // Configure Table Columns using cell value factories
        idColumn.setCellValueFactory(cellData -> new SimpleIntegerProperty(cellData.getValue().getId() != null ? cellData.getValue().getId() : 0));
        typeColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getType().getDisplayName()));
        brandColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getBrand()));
        nameColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getName()));
        priceColumn.setCellValueFactory(cellData -> new SimpleDoubleProperty(cellData.getValue().getPrice()));
        tdpColumn.setCellValueFactory(cellData -> new SimpleIntegerProperty(cellData.getValue().getTdpWatts()));
        specsColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getKeySpecs()));

        // Format price column with currency
        priceColumn.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(Number price, boolean empty) {
                super.updateItem(price, empty);
                if (empty || price == null) {
                    setText(null);
                } else {
                    setText(String.format("$%.2f", price.doubleValue()));
                }
            }
        });

        // Initialize Filter ComboBox
        typeFilterComboBox.getItems().add("All Categories");
        for (ComponentType type : ComponentType.values()) {
            typeFilterComboBox.getItems().add(type.getDisplayName());
        }
        typeFilterComboBox.getSelectionModel().selectFirst();

        // Setup FilteredList wrapped around masterData
        filteredData = new FilteredList<>(masterData, p -> true);

        // Listen for filter and search input changes
        typeFilterComboBox.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> updatePredicate());
        searchTextField.textProperty().addListener((obs, oldVal, newVal) -> updatePredicate());

        // Wrap FilteredList in SortedList to allow column sorting
        SortedList<Component> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(componentTableView.comparatorProperty());
        componentTableView.setItems(sortedData);

        // Update button states on selection
        componentTableView.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            boolean hasSelection = (newSel != null);
            addToBuildButton.setDisable(!hasSelection);
            deleteButton.setDisable(!hasSelection);
        });
        addToBuildButton.setDisable(true);
        deleteButton.setDisable(true);
    }

    public void setComponentDao(ComponentDao componentDao) {
        this.componentDao = componentDao;
        loadComponentsFromDatabase();
    }

    public void setOnAddToBuildCallback(Consumer<Component> callback) {
        this.onAddToBuildCallback = callback;
    }

    public void setOnStatusMessageCallback(Consumer<String> callback) {
        this.onStatusMessageCallback = callback;
    }

    public void loadComponentsFromDatabase() {
        if (componentDao == null) return;
        List<Component> components = componentDao.findAll();
        masterData.setAll(components);
        updateSummaryLabel();
        if (onStatusMessageCallback != null) {
            onStatusMessageCallback.accept("Loaded " + components.size() + " components from database.");
        }
    }

    private void updatePredicate() {
        String selectedCategory = typeFilterComboBox.getValue();
        String searchFilter = searchTextField.getText() != null ? searchTextField.getText().trim().toLowerCase() : "";

        filteredData.setPredicate(comp -> {
            // Category check
            if (selectedCategory != null && !selectedCategory.equals("All Categories")) {
                if (!comp.getType().getDisplayName().equalsIgnoreCase(selectedCategory)) {
                    return false;
                }
            }

            // Search filter check (brand or name or key specs)
            if (!searchFilter.isEmpty()) {
                boolean matchesBrand = comp.getBrand().toLowerCase().contains(searchFilter);
                boolean matchesName = comp.getName().toLowerCase().contains(searchFilter);
                boolean matchesSpecs = comp.getKeySpecs().toLowerCase().contains(searchFilter);
                return matchesBrand || matchesName || matchesSpecs;
            }

            return true;
        });

        updateSummaryLabel();
    }

    private void updateSummaryLabel() {
        tableSummaryLabel.setText(String.format("Showing %d of %d components", filteredData.size(), masterData.size()));
    }

    @FXML
    private void handleResetFilters() {
        typeFilterComboBox.getSelectionModel().selectFirst();
        searchTextField.clear();
    }

    @FXML
    private void handleRefresh() {
        loadComponentsFromDatabase();
    }

    @FXML
    private void handleAddToBuild() {
        Component selected = componentTableView.getSelectionModel().getSelectedItem();
        if (selected != null && onAddToBuildCallback != null) {
            onAddToBuildCallback.accept(selected);
            if (onStatusMessageCallback != null) {
                onStatusMessageCallback.accept("Added " + selected.getBrand() + " " + selected.getName() + " to active build.");
            }
        }
    }

    @FXML
    private void handleDeleteComponent() {
        Component selected = componentTableView.getSelectionModel().getSelectedItem();
        if (selected == null || componentDao == null) return;

        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Confirm Deletion");
        confirmAlert.setHeaderText("Delete " + selected.getBrand() + " " + selected.getName() + "?");
        confirmAlert.setContentText("This will permanently remove the component and its specifications from the database.");

        confirmAlert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                boolean success = componentDao.deleteById(selected.getId());
                if (success) {
                    masterData.remove(selected);
                    updateSummaryLabel();
                    if (onStatusMessageCallback != null) {
                        onStatusMessageCallback.accept("Deleted " + selected.getName() + ".");
                    }
                } else {
                    Alert errorAlert = new Alert(Alert.AlertType.ERROR, "Failed to delete component from database.");
                    errorAlert.showAndWait();
                }
            }
        });
    }

    @FXML
    private void handleAddComponent() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/pcanalyzer/view/add-component-dialog.fxml"));
            Parent root = loader.load();

            AddComponentDialogController controller = loader.getController();
            controller.setComponentDao(componentDao);

            Stage dialogStage = new Stage();
            dialogStage.setTitle("Add New Component");
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/com/pcanalyzer/css/styles.css").toExternalForm());
            dialogStage.setScene(scene);
            controller.setDialogStage(dialogStage);

            dialogStage.showAndWait();

            if (controller.isComponentAdded()) {
                loadComponentsFromDatabase();
                if (onStatusMessageCallback != null) {
                    onStatusMessageCallback.accept("New component successfully created and persisted.");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR, "Could not open Add Component dialog: " + e.getMessage());
            alert.showAndWait();
        }
    }
}
