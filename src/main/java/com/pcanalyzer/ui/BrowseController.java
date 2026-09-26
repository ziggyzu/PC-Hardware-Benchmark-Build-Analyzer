package com.pcanalyzer.ui;

import com.pcanalyzer.db.ComponentDao;
import com.pcanalyzer.model.Component;
import com.pcanalyzer.model.ComponentType;
import javafx.beans.binding.Bindings;
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
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;

// Controller for browsing, searching, and managing hardware parts in the catalog
public class BrowseController {

    // Main layout container
    @FXML private SplitPane browseSplitPane;

    // Search and filter controls
    @FXML private ComboBox<String> typeFilterComboBox;
    @FXML private TextField searchTextField;
    @FXML private Slider priceSlider;
    @FXML private Label priceSliderLabel;
    @FXML private CheckBox lowTdpCheckBox;
    @FXML private FlowPane quickFilterFlowPane;

    // Table view and its columns
    @FXML private TableView<Component> componentTableView;
    @FXML private TableColumn<Component, Number> idColumn;
    @FXML private TableColumn<Component, String> typeColumn;
    @FXML private TableColumn<Component, String> brandColumn;
    @FXML private TableColumn<Component, String> nameColumn;
    @FXML private TableColumn<Component, Number> priceColumn;
    @FXML private TableColumn<Component, Number> tdpColumn;
    @FXML private TableColumn<Component, String> specsColumn;

    // Action buttons at the bottom of the table
    @FXML private Button addToBuildButton;
    @FXML private Button deleteButton;
    @FXML private Label tableSummaryLabel;

    // Side panel showing details of the currently selected part
    @FXML private StackPane inspectorStackPane;
    @FXML private VBox inspectorPlaceholder;
    @FXML private VBox inspectorDetails;
    @FXML private Label detailTypeLabel;
    @FXML private Label detailNameLabel;
    @FXML private Label detailBrandLabel;
    @FXML private Label detailPriceLabel;
    @FXML private Label detailTdpLabel;
    @FXML private Label detailSpecsLabel;
    @FXML private Label detailTdpPercentLabel;
    @FXML private ProgressBar tdpProgressBar;
    @FXML private Button inspectorAddToBuildBtn;

    // Internal data lists and database connection
    private ComponentDao componentDao;
    private final ObservableList<Component> masterData = FXCollections.observableArrayList();
    private FilteredList<Component> filteredData;
    private Consumer<Component> onAddToBuildCallback;
    private Consumer<String> onStatusMessageCallback;

    // Set up table columns, listeners, and filters
    @FXML
    public void initialize() {
        // Read each field from a component object and place it into the matching column
        idColumn.setCellValueFactory(cellData -> new SimpleIntegerProperty(cellData.getValue().getId() != null ? cellData.getValue().getId() : 0));
        typeColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getType().getDisplayName()));
        brandColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getBrand()));
        nameColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getName()));
        priceColumn.setCellValueFactory(cellData -> new SimpleDoubleProperty(cellData.getValue().getPrice()));
        tdpColumn.setCellValueFactory(cellData -> new SimpleIntegerProperty(cellData.getValue().getTdpWatts()));
        specsColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getKeySpecs()));

        // Format the price column so it shows a dollar sign
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

        // Automatically resize columns when the window expands or shrinks
        idColumn.prefWidthProperty().bind(componentTableView.widthProperty().multiply(0.06));
        typeColumn.prefWidthProperty().bind(componentTableView.widthProperty().multiply(0.12));
        brandColumn.prefWidthProperty().bind(componentTableView.widthProperty().multiply(0.12));
        nameColumn.prefWidthProperty().bind(componentTableView.widthProperty().multiply(0.24));
        priceColumn.prefWidthProperty().bind(componentTableView.widthProperty().multiply(0.11));
        tdpColumn.prefWidthProperty().bind(componentTableView.widthProperty().multiply(0.08));
        specsColumn.prefWidthProperty().bind(componentTableView.widthProperty().multiply(0.26));

        // Fill the category dropdown with all component types
        typeFilterComboBox.getItems().add("All Categories");
        for (ComponentType type : ComponentType.values()) {
            typeFilterComboBox.getItems().add(type.getDisplayName());
        }
        typeFilterComboBox.getSelectionModel().selectFirst();

        // Update the slider's price label in real time as the thumb moves
        priceSliderLabel.textProperty().bind(
                Bindings.format("$%.0f", priceSlider.valueProperty())
        );

        // Watch for changes in search text or filter controls
        filteredData = new FilteredList<>(masterData, p -> true);
        typeFilterComboBox.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> updatePredicate());
        searchTextField.textProperty().addListener((obs, oldVal, newVal) -> updatePredicate());
        priceSlider.valueProperty().addListener((obs, oldVal, newVal) -> updatePredicate());
        lowTdpCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> updatePredicate());

        // Allow users to sort table columns by clicking their headers
        SortedList<Component> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(componentTableView.comparatorProperty());
        componentTableView.setItems(sortedData);

        // Update action buttons and inspector when a row is clicked
        componentTableView.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            boolean hasSelection = (newSel != null);
            addToBuildButton.setDisable(!hasSelection);
            deleteButton.setDisable(!hasSelection);
            updateInspector(newSel);
        });

        addToBuildButton.setDisable(true);
        deleteButton.setDisable(true);
    }

    // Connect the database DAO and start loading parts
    public void setComponentDao(ComponentDao componentDao) {
        this.componentDao = componentDao;
        loadComponentsFromDatabase();
    }

    // Set callback to notify the parent window when a part is added to a build
    public void setOnAddToBuildCallback(Consumer<Component> callback) {
        this.onAddToBuildCallback = callback;
    }

    // Set callback to post messages to the bottom status bar
    public void setOnStatusMessageCallback(Consumer<String> callback) {
        this.onStatusMessageCallback = callback;
    }

    // Fetch all parts from SQLite in the background so the UI doesn't stutter
    public void loadComponentsFromDatabase() {
        if (componentDao == null) return;

        if (onStatusMessageCallback != null) {
            onStatusMessageCallback.accept("Querying database on background thread pool...");
        }

        // Run the database query on a background worker thread
        javafx.concurrent.Task<List<Component>> loadTask = new javafx.concurrent.Task<>() {
            @Override
            protected List<Component> call() {
                return componentDao.findAll();
            }
        };

        // When the query finishes, update the table on the screen
        loadTask.setOnSucceeded(event -> {
            List<Component> components = loadTask.getValue();
            masterData.setAll(components);
            updateSummaryLabel();
            if (onStatusMessageCallback != null) {
                onStatusMessageCallback.accept(String.format("Loaded %d components asynchronously from SQLite.", components.size()));
            }
        });

        // Show an error if the query fails
        loadTask.setOnFailed(event -> {
            Throwable ex = loadTask.getException();
            System.err.println("[BrowseController] Background loading failed: " + ex.getMessage());
            if (onStatusMessageCallback != null) {
                onStatusMessageCallback.accept("Error loading components: " + ex.getMessage());
            }
        });

        com.pcanalyzer.util.ThreadPoolManager.getInstance().submitTask(loadTask);
    }

    // Filter parts according to search text, selected category, max price, and TDP
    private void updatePredicate() {
        String selectedCategory = typeFilterComboBox.getValue();
        String searchFilter = searchTextField.getText() != null ? searchTextField.getText().trim().toLowerCase() : "";
        double maxPrice = priceSlider.getValue();
        boolean lowTdpOnly = lowTdpCheckBox.isSelected();

        filteredData.setPredicate(comp -> {
            // Check if the part matches the selected category
            if (selectedCategory != null && !selectedCategory.equals("All Categories")) {
                if (!comp.getType().getDisplayName().equalsIgnoreCase(selectedCategory)) {
                    return false;
                }
            }

            // Exclude parts that cost more than the price slider
            if (comp.getPrice() > maxPrice) {
                return false;
            }

            // Exclude power-hungry parts if the low power checkbox is on
            if (lowTdpOnly && comp.getTdpWatts() >= 100) {
                return false;
            }

            // Check if the part name, brand, or specs match what the user typed
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

    // Update the right-side inspector panel with details of the selected part
    private void updateInspector(Component comp) {
        if (comp == null) {
            // Show placeholder if nothing is selected
            inspectorPlaceholder.setVisible(true);
            inspectorDetails.setVisible(false);
        } else {
            // Populate all fields with the selected part's specs
            inspectorPlaceholder.setVisible(false);
            inspectorDetails.setVisible(true);

            detailTypeLabel.setText(comp.getType().getDisplayName().toUpperCase());
            detailNameLabel.setText(comp.getName());
            detailBrandLabel.setText("Brand: " + comp.getBrand());
            detailPriceLabel.setText(String.format("$%.2f", comp.getPrice()));
            detailTdpLabel.setText(comp.getTdpWatts() + " W");
            detailSpecsLabel.setText(comp.getKeySpecs());

            // Show power draw percentage relative to 350W
            double tdpRatio = Math.min(1.0, (double) comp.getTdpWatts() / 350.0);
            tdpProgressBar.setProgress(tdpRatio);
            detailTdpPercentLabel.setText(String.format("%.0f%%", tdpRatio * 100));
        }
    }

    // Update the label showing how many parts match the current filters
    private void updateSummaryLabel() {
        tableSummaryLabel.setText(String.format("Showing %d of %d components", filteredData.size(), masterData.size()));
    }

    // Quick filter tag button handlers
    @FXML private void handleQuickFilterAll() { typeFilterComboBox.getSelectionModel().select(0); }
    @FXML private void handleQuickFilterCpu() { selectCategory(ComponentType.CPU.getDisplayName()); }
    @FXML private void handleQuickFilterGpu() { selectCategory(ComponentType.GPU.getDisplayName()); }
    @FXML private void handleQuickFilterMobo() { selectCategory(ComponentType.MOTHERBOARD.getDisplayName()); }
    @FXML private void handleQuickFilterRam() { selectCategory(ComponentType.RAM.getDisplayName()); }
    @FXML private void handleQuickFilterPsu() { selectCategory(ComponentType.PSU.getDisplayName()); }

    // Helper to select a category by its name
    private void selectCategory(String displayName) {
        typeFilterComboBox.getSelectionModel().select(displayName);
    }

    // Reset all search boxes, sliders, and checkboxes back to default
    @FXML
    private void handleResetFilters() {
        typeFilterComboBox.getSelectionModel().selectFirst();
        searchTextField.clear();
        priceSlider.setValue(1000.0);
        lowTdpCheckBox.setSelected(false);
    }

    // Reload all parts from the database
    @FXML
    private void handleRefresh() {
        loadComponentsFromDatabase();
    }

    // Add the currently highlighted part to the active PC build
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

    // Delete the selected part after asking the user to confirm
    @FXML
    private void handleDeleteComponent() {
        Component selected = componentTableView.getSelectionModel().getSelectedItem();
        if (selected == null || componentDao == null) return;

        // Ask for confirmation before deleting
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
                    updateInspector(null);
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

    // Open a popup dialog to let the user add a new hardware part
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

            // Refresh the table if a new part was saved
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
