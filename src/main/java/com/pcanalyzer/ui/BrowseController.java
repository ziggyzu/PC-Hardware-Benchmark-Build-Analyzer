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

/**
 * Controller for the Hardware Catalog browse and search view.
 *
 * Demonstrates:
 * 1. Rich JavaFX UI Design: SplitPane, FlowPane, Slider, CheckBox, TableView, TitledPane, StackPane, ProgressBar, Tooltips.
 * 2. Dynamic Layout Responsiveness: TableColumn widths proportionally bound to TableView width via property constraints.
 * 3. High-Performance Filtering: FilteredList + SortedList reacting to search, sliders, and checkboxes.
 */
public class BrowseController {

    // Main layout
    @FXML private SplitPane browseSplitPane;

    // Filters and Toolbar
    @FXML private ComboBox<String> typeFilterComboBox;
    @FXML private TextField searchTextField;
    @FXML private Slider priceSlider;
    @FXML private Label priceSliderLabel;
    @FXML private CheckBox lowTdpCheckBox;
    @FXML private FlowPane quickFilterFlowPane;

    // Catalog Table
    @FXML private TableView<Component> componentTableView;
    @FXML private TableColumn<Component, Number> idColumn;
    @FXML private TableColumn<Component, String> typeColumn;
    @FXML private TableColumn<Component, String> brandColumn;
    @FXML private TableColumn<Component, String> nameColumn;
    @FXML private TableColumn<Component, Number> priceColumn;
    @FXML private TableColumn<Component, Number> tdpColumn;
    @FXML private TableColumn<Component, String> specsColumn;

    // Bottom Actions
    @FXML private Button addToBuildButton;
    @FXML private Button deleteButton;
    @FXML private Label tableSummaryLabel;

    // Inspector Panel
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

    private ComponentDao componentDao;
    private final ObservableList<Component> masterData = FXCollections.observableArrayList();
    private FilteredList<Component> filteredData;
    private Consumer<Component> onAddToBuildCallback;
    private Consumer<String> onStatusMessageCallback;

    @FXML
    public void initialize() {
        // 1. Configure Table Columns with Value Factories
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

        // 2. Dynamic Layout Responsiveness: Bind column widths proportionally to TableView width
        idColumn.prefWidthProperty().bind(componentTableView.widthProperty().multiply(0.06));
        typeColumn.prefWidthProperty().bind(componentTableView.widthProperty().multiply(0.12));
        brandColumn.prefWidthProperty().bind(componentTableView.widthProperty().multiply(0.12));
        nameColumn.prefWidthProperty().bind(componentTableView.widthProperty().multiply(0.24));
        priceColumn.prefWidthProperty().bind(componentTableView.widthProperty().multiply(0.11));
        tdpColumn.prefWidthProperty().bind(componentTableView.widthProperty().multiply(0.08));
        specsColumn.prefWidthProperty().bind(componentTableView.widthProperty().multiply(0.26));

        // 3. Initialize Filter ComboBox
        typeFilterComboBox.getItems().add("All Categories");
        for (ComponentType type : ComponentType.values()) {
            typeFilterComboBox.getItems().add(type.getDisplayName());
        }
        typeFilterComboBox.getSelectionModel().selectFirst();

        // 4. Live Property Binding for Slider Label
        priceSliderLabel.textProperty().bind(
                Bindings.format("$%.0f", priceSlider.valueProperty())
        );

        // 5. Setup FilteredList & Listeners
        filteredData = new FilteredList<>(masterData, p -> true);
        typeFilterComboBox.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> updatePredicate());
        searchTextField.textProperty().addListener((obs, oldVal, newVal) -> updatePredicate());
        priceSlider.valueProperty().addListener((obs, oldVal, newVal) -> updatePredicate());
        lowTdpCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> updatePredicate());

        // 6. Wrap in SortedList for interactive column sorting
        SortedList<Component> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(componentTableView.comparatorProperty());
        componentTableView.setItems(sortedData);

        // 7. Selection Listeners for Table & Inspector Card
        componentTableView.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            boolean hasSelection = (newSel != null);
            addToBuildButton.setDisable(!hasSelection);
            deleteButton.setDisable(!hasSelection);
            updateInspector(newSel);
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
        double maxPrice = priceSlider.getValue();
        boolean lowTdpOnly = lowTdpCheckBox.isSelected();

        filteredData.setPredicate(comp -> {
            // Category check
            if (selectedCategory != null && !selectedCategory.equals("All Categories")) {
                if (!comp.getType().getDisplayName().equalsIgnoreCase(selectedCategory)) {
                    return false;
                }
            }

            // Price filter check
            if (comp.getPrice() > maxPrice) {
                return false;
            }

            // Low TDP check (< 100W)
            if (lowTdpOnly && comp.getTdpWatts() >= 100) {
                return false;
            }

            // Search keyword check
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

    private void updateInspector(Component comp) {
        if (comp == null) {
            inspectorPlaceholder.setVisible(true);
            inspectorDetails.setVisible(false);
        } else {
            inspectorPlaceholder.setVisible(false);
            inspectorDetails.setVisible(true);

            detailTypeLabel.setText(comp.getType().getDisplayName().toUpperCase());
            detailNameLabel.setText(comp.getName());
            detailBrandLabel.setText("Brand: " + comp.getBrand());
            detailPriceLabel.setText(String.format("$%.2f", comp.getPrice()));
            detailTdpLabel.setText(comp.getTdpWatts() + " W");
            detailSpecsLabel.setText(comp.getKeySpecs());

            // Relative TDP power draw progress (compared against high-end 350W target)
            double tdpRatio = Math.min(1.0, (double) comp.getTdpWatts() / 350.0);
            tdpProgressBar.setProgress(tdpRatio);
            detailTdpPercentLabel.setText(String.format("%.0f%%", tdpRatio * 100));
        }
    }

    private void updateSummaryLabel() {
        tableSummaryLabel.setText(String.format("Showing %d of %d components", filteredData.size(), masterData.size()));
    }

    // Quick tag handlers
    @FXML private void handleQuickFilterAll() { typeFilterComboBox.getSelectionModel().select(0); }
    @FXML private void handleQuickFilterCpu() { selectCategory(ComponentType.CPU.getDisplayName()); }
    @FXML private void handleQuickFilterGpu() { selectCategory(ComponentType.GPU.getDisplayName()); }
    @FXML private void handleQuickFilterMobo() { selectCategory(ComponentType.MOTHERBOARD.getDisplayName()); }
    @FXML private void handleQuickFilterRam() { selectCategory(ComponentType.RAM.getDisplayName()); }
    @FXML private void handleQuickFilterPsu() { selectCategory(ComponentType.PSU.getDisplayName()); }

    private void selectCategory(String displayName) {
        typeFilterComboBox.getSelectionModel().select(displayName);
    }

    @FXML
    private void handleResetFilters() {
        typeFilterComboBox.getSelectionModel().selectFirst();
        searchTextField.clear();
        priceSlider.setValue(1000.0);
        lowTdpCheckBox.setSelected(false);
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
