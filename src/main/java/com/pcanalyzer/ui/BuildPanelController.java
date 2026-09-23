package com.pcanalyzer.ui;

import com.pcanalyzer.model.*;
import com.pcanalyzer.service.CompatibilityChecker;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;

/**
 * Controller for the Build Configurator panel.
 *
 * Demonstrates:
 * 1. Rich JavaFX UI Design: SplitPane, Accordion, TitledPane, ProgressBar, Slider, CheckBox, ListView, Badges.
 * 2. Dynamic Layout Responsiveness: SplitPane divider constraints, reactive power and budget utilization meters.
 * 3. Reactive Feedback: Real-time recalculation of costs, power metrics, and compatibility diagnostics.
 */
public class BuildPanelController {

    // Main layout
    @FXML private SplitPane buildSplitPane;

    // Component Accordion & TitledPanes
    @FXML private Accordion componentsAccordion;
    @FXML private TitledPane cpuTitledPane;
    @FXML private TitledPane moboTitledPane;
    @FXML private TitledPane ramTitledPane;
    @FXML private TitledPane gpuTitledPane;
    @FXML private TitledPane psuTitledPane;

    // Slot Labels
    @FXML private Label cpuNameLabel;
    @FXML private Label cpuSpecsLabel;
    @FXML private Label cpuPriceLabel;

    @FXML private Label moboNameLabel;
    @FXML private Label moboSpecsLabel;
    @FXML private Label moboPriceLabel;

    @FXML private Label ramNameLabel;
    @FXML private Label ramSpecsLabel;
    @FXML private Label ramPriceLabel;

    @FXML private Label gpuNameLabel;
    @FXML private Label gpuSpecsLabel;
    @FXML private Label gpuPriceLabel;

    @FXML private Label psuNameLabel;
    @FXML private Label psuSpecsLabel;
    @FXML private Label psuPriceLabel;

    // Financial & Budget Tracker Controls
    @FXML private Label totalPriceLabel;
    @FXML private Label budgetValueLabel;
    @FXML private Label budgetPercentLabel;
    @FXML private ProgressBar budgetProgressBar;
    @FXML private Slider budgetSlider;

    // Power Draw Controls
    @FXML private Label totalTdpLabel;
    @FXML private Label estimatedPeakLabel;
    @FXML private Label recommendedPsuLabel;
    @FXML private Label psuLoadPercentLabel;
    @FXML private ProgressBar psuLoadProgressBar;
    @FXML private CheckBox overclockHeadroomCheckBox;

    // Diagnostics
    @FXML private Label compatibilityBadge;
    @FXML private ListView<CompatibilityIssue> diagnosticListView;

    private final Build currentBuild = new Build("Active System Build");
    private final CompatibilityChecker compatibilityChecker = new CompatibilityChecker();

    @FXML
    public void initialize() {
        // 1. Custom cell renderer for compatibility diagnostics
        diagnosticListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(CompatibilityIssue issue, boolean empty) {
                super.updateItem(issue, empty);
                if (empty || issue == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(String.format("[%s] %s: %s", issue.getSeverity().getLabel(), issue.getRuleName(), issue.getMessage()));
                    if (issue.getSeverity() == CompatibilityStatus.INCOMPATIBLE) {
                        setStyle("-fx-text-fill: #dc2626; -fx-font-weight: bold;");
                    } else {
                        setStyle("-fx-text-fill: #ca8a04;");
                    }
                }
            }
        });

        // 2. Budget slider live binding
        budgetValueLabel.textProperty().bind(
                Bindings.format("$%.0f", budgetSlider.valueProperty())
        );
        budgetSlider.valueProperty().addListener((obs, oldVal, newVal) -> updateBudgetMetrics());
        overclockHeadroomCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> updatePowerMetrics());

        refreshBuildView();
    }

    /**
     * Add a component to the active build and refresh view.
     */
    public void addComponentToBuild(Component component) {
        if (component == null) return;
        currentBuild.setComponent(component);
        refreshBuildView();
    }

    public Build getCurrentBuild() {
        return currentBuild;
    }

    @FXML
    private void handleRemoveCpu() {
        currentBuild.setCpu(null);
        refreshBuildView();
    }

    @FXML
    private void handleRemoveMotherboard() {
        currentBuild.setMotherboard(null);
        refreshBuildView();
    }

    @FXML
    private void handleRemoveRam() {
        currentBuild.setRam(null);
        refreshBuildView();
    }

    @FXML
    private void handleRemoveGpu() {
        currentBuild.setGpu(null);
        refreshBuildView();
    }

    @FXML
    private void handleRemovePsu() {
        currentBuild.setPsu(null);
        refreshBuildView();
    }

    @FXML
    private void handleClearBuild() {
        currentBuild.clear();
        refreshBuildView();
    }

    @FXML
    private void handleExpandAll() {
        // Expand the currently selected or first pane
        if (componentsAccordion != null && !componentsAccordion.getPanes().isEmpty()) {
            componentsAccordion.setExpandedPane(componentsAccordion.getPanes().get(0));
        }
    }

    /**
     * Synchronizes UI controls with current Build state and evaluates compatibility.
     */
    public void refreshBuildView() {
        // CPU Slot
        Cpu cpu = currentBuild.getCpu();
        if (cpu != null) {
            cpuTitledPane.setText("Processor (CPU) — " + cpu.getBrand() + " " + cpu.getName());
            cpuNameLabel.setText(cpu.getBrand() + " " + cpu.getName());
            cpuSpecsLabel.setText(cpu.getKeySpecs());
            cpuPriceLabel.setText(String.format("$%.2f", cpu.getPrice()));
        } else {
            cpuTitledPane.setText("Processor (CPU) — None Selected");
            cpuNameLabel.setText("No CPU selected");
            cpuSpecsLabel.setText("Select a CPU from the Hardware Catalog");
            cpuPriceLabel.setText("$0.00");
        }

        // Motherboard Slot
        Motherboard mobo = currentBuild.getMotherboard();
        if (mobo != null) {
            moboTitledPane.setText("Motherboard — " + mobo.getBrand() + " " + mobo.getName());
            moboNameLabel.setText(mobo.getBrand() + " " + mobo.getName());
            moboSpecsLabel.setText(mobo.getKeySpecs());
            moboPriceLabel.setText(String.format("$%.2f", mobo.getPrice()));
        } else {
            moboTitledPane.setText("Motherboard — None Selected");
            moboNameLabel.setText("No Motherboard selected");
            moboSpecsLabel.setText("Select a Motherboard from the Hardware Catalog");
            moboPriceLabel.setText("$0.00");
        }

        // RAM Slot
        Ram ram = currentBuild.getRam();
        if (ram != null) {
            ramTitledPane.setText("Memory (RAM) — " + ram.getBrand() + " " + ram.getName());
            ramNameLabel.setText(ram.getBrand() + " " + ram.getName());
            ramSpecsLabel.setText(ram.getKeySpecs());
            ramPriceLabel.setText(String.format("$%.2f", ram.getPrice()));
        } else {
            ramTitledPane.setText("Memory (RAM) — None Selected");
            ramNameLabel.setText("No RAM selected");
            ramSpecsLabel.setText("Select RAM from the Hardware Catalog");
            ramPriceLabel.setText("$0.00");
        }

        // GPU Slot
        Gpu gpu = currentBuild.getGpu();
        if (gpu != null) {
            gpuTitledPane.setText("Graphics Card (GPU) — " + gpu.getBrand() + " " + gpu.getName());
            gpuNameLabel.setText(gpu.getBrand() + " " + gpu.getName());
            gpuSpecsLabel.setText(gpu.getKeySpecs());
            gpuPriceLabel.setText(String.format("$%.2f", gpu.getPrice()));
        } else {
            gpuTitledPane.setText("Graphics Card (GPU) — None Selected");
            gpuNameLabel.setText("No GPU selected");
            gpuSpecsLabel.setText("Select a GPU from the Hardware Catalog");
            gpuPriceLabel.setText("$0.00");
        }

        // PSU Slot
        Psu psu = currentBuild.getPsu();
        if (psu != null) {
            psuTitledPane.setText("Power Supply (PSU) — " + psu.getBrand() + " " + psu.getName());
            psuNameLabel.setText(psu.getBrand() + " " + psu.getName());
            psuSpecsLabel.setText(psu.getKeySpecs());
            psuPriceLabel.setText(String.format("$%.2f", psu.getPrice()));
        } else {
            psuTitledPane.setText("Power Supply (PSU) — None Selected");
            psuNameLabel.setText("No PSU selected");
            psuSpecsLabel.setText("Select a PSU from the Hardware Catalog");
            psuPriceLabel.setText("$0.00");
        }

        // Update financial and power progress indicators
        updateBudgetMetrics();
        updatePowerMetrics();

        // Compatibility Evaluation
        CompatibilityResult result = compatibilityChecker.checkCompatibility(currentBuild);
        updateCompatibilityDisplay(result);
    }

    private void updateBudgetMetrics() {
        double totalPrice = currentBuild.getTotalPrice();
        totalPriceLabel.setText(String.format("$%.2f", totalPrice));

        double budgetLimit = (budgetSlider != null) ? budgetSlider.getValue() : 1500.0;
        double budgetRatio = budgetLimit > 0 ? (totalPrice / budgetLimit) : 0.0;

        if (budgetProgressBar != null) {
            budgetProgressBar.setProgress(Math.min(1.0, budgetRatio));
        }
        if (budgetPercentLabel != null) {
            budgetPercentLabel.setText(String.format("%.0f%% of budget", budgetRatio * 100));
            if (budgetRatio > 1.0) {
                budgetPercentLabel.setStyle("-fx-text-fill: #dc2626; -fx-font-weight: bold;");
            } else {
                budgetPercentLabel.setStyle("-fx-text-fill: #16a34a; -fx-font-weight: bold;");
            }
        }
    }

    private void updatePowerMetrics() {
        int baseTdp = currentBuild.getTotalTdpWatts();
        int peakWatts = currentBuild.getEstimatedPeakPowerWatts();

        boolean extraOverclock = (overclockHeadroomCheckBox != null && overclockHeadroomCheckBox.isSelected());
        int recWatts = currentBuild.getRecommendedPsuWatts();
        if (extraOverclock) {
            recWatts = (int) Math.round(recWatts * 1.15);
        }

        totalTdpLabel.setText(baseTdp + " W");
        estimatedPeakLabel.setText(peakWatts + " W");
        recommendedPsuLabel.setText(recWatts + " W" + (extraOverclock ? " (+35% OC Headroom)" : " (+20% Headroom)"));

        // Update PSU utilization meter
        Psu psu = currentBuild.getPsu();
        if (psu != null && psu.getWattage() > 0) {
            double loadRatio = (double) peakWatts / psu.getWattage();
            psuLoadProgressBar.setProgress(Math.min(1.0, loadRatio));
            psuLoadPercentLabel.setText(String.format("%.0f%% of %d W", loadRatio * 100, psu.getWattage()));

            if (loadRatio > 1.0) {
                psuLoadPercentLabel.setStyle("-fx-text-fill: #dc2626; -fx-font-weight: bold;");
            } else if (loadRatio > 0.8) {
                psuLoadPercentLabel.setStyle("-fx-text-fill: #ca8a04; -fx-font-weight: bold;");
            } else {
                psuLoadPercentLabel.setStyle("-fx-text-fill: #16a34a; -fx-font-weight: bold;");
            }
        } else {
            psuLoadProgressBar.setProgress(0.0);
            psuLoadPercentLabel.setText("No PSU selected");
            psuLoadPercentLabel.setStyle("");
        }
    }

    private void updateCompatibilityDisplay(CompatibilityResult result) {
        diagnosticListView.setItems(FXCollections.observableArrayList(result.getIssues()));

        compatibilityBadge.getStyleClass().removeAll("badge-compatible", "badge-warning", "badge-incompatible");
        switch (result.getStatus()) {
            case COMPATIBLE -> {
                compatibilityBadge.setText("Compatible");
                compatibilityBadge.getStyleClass().add("badge-compatible");
            }
            case WARNING -> {
                compatibilityBadge.setText("Warning");
                compatibilityBadge.getStyleClass().add("badge-warning");
            }
            case INCOMPATIBLE -> {
                compatibilityBadge.setText("Incompatible");
                compatibilityBadge.getStyleClass().add("badge-incompatible");
            }
        }
    }
}
