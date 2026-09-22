package com.pcanalyzer.ui;

import com.pcanalyzer.model.*;
import com.pcanalyzer.service.CompatibilityChecker;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;

/**
 * Controller for the Build Configurator panel.
 *
 * Design Decision:
 * 1. Thin Controller: Delegates domain calculations to Build and compatibility verification
 *    to CompatibilityChecker.
 * 2. Real-Time Reactive Feedback: Every mutation to the active build triggers an immediate
 *    recalculation of costs, power draw, and compatibility diagnostics.
 */
public class BuildPanelController {

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

    // Summary & Diagnostics
    @FXML private Label totalPriceLabel;
    @FXML private Label totalTdpLabel;
    @FXML private Label estimatedPeakLabel;
    @FXML private Label recommendedPsuLabel;
    @FXML private Label compatibilityBadge;
    @FXML private ListView<CompatibilityIssue> diagnosticListView;

    private final Build currentBuild = new Build("Active System Build");
    private final CompatibilityChecker compatibilityChecker = new CompatibilityChecker();

    @FXML
    public void initialize() {
        // Custom cell renderer for compatibility diagnostics
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
                        setStyle("-fx-text-fill: #b22222; -fx-font-weight: bold;");
                    } else {
                        setStyle("-fx-text-fill: #8b8000;");
                    }
                }
            }
        });

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

    /**
     * Synchronizes UI controls with current Build state and evaluates compatibility.
     */
    public void refreshBuildView() {
        // CPU Slot
        Cpu cpu = currentBuild.getCpu();
        if (cpu != null) {
            cpuNameLabel.setText(cpu.getBrand() + " " + cpu.getName());
            cpuSpecsLabel.setText(cpu.getKeySpecs());
            cpuPriceLabel.setText(String.format("$%.2f", cpu.getPrice()));
        } else {
            cpuNameLabel.setText("No CPU selected");
            cpuSpecsLabel.setText("Select a CPU from the Hardware Catalog");
            cpuPriceLabel.setText("$0.00");
        }

        // Motherboard Slot
        Motherboard mobo = currentBuild.getMotherboard();
        if (mobo != null) {
            moboNameLabel.setText(mobo.getBrand() + " " + mobo.getName());
            moboSpecsLabel.setText(mobo.getKeySpecs());
            moboPriceLabel.setText(String.format("$%.2f", mobo.getPrice()));
        } else {
            moboNameLabel.setText("No Motherboard selected");
            moboSpecsLabel.setText("Select a Motherboard from the Hardware Catalog");
            moboPriceLabel.setText("$0.00");
        }

        // RAM Slot
        Ram ram = currentBuild.getRam();
        if (ram != null) {
            ramNameLabel.setText(ram.getBrand() + " " + ram.getName());
            ramSpecsLabel.setText(ram.getKeySpecs());
            ramPriceLabel.setText(String.format("$%.2f", ram.getPrice()));
        } else {
            ramNameLabel.setText("No RAM selected");
            ramSpecsLabel.setText("Select RAM from the Hardware Catalog");
            ramPriceLabel.setText("$0.00");
        }

        // GPU Slot
        Gpu gpu = currentBuild.getGpu();
        if (gpu != null) {
            gpuNameLabel.setText(gpu.getBrand() + " " + gpu.getName());
            gpuSpecsLabel.setText(gpu.getKeySpecs());
            gpuPriceLabel.setText(String.format("$%.2f", gpu.getPrice()));
        } else {
            gpuNameLabel.setText("No GPU selected");
            gpuSpecsLabel.setText("Select a GPU from the Hardware Catalog");
            gpuPriceLabel.setText("$0.00");
        }

        // PSU Slot
        Psu psu = currentBuild.getPsu();
        if (psu != null) {
            psuNameLabel.setText(psu.getBrand() + " " + psu.getName());
            psuSpecsLabel.setText(psu.getKeySpecs());
            psuPriceLabel.setText(String.format("$%.2f", psu.getPrice()));
        } else {
            psuNameLabel.setText("No PSU selected");
            psuSpecsLabel.setText("Select a PSU from the Hardware Catalog");
            psuPriceLabel.setText("$0.00");
        }

        // Build Calculations
        totalPriceLabel.setText(String.format("$%.2f", currentBuild.getTotalPrice()));
        totalTdpLabel.setText(currentBuild.getTotalTdpWatts() + " W");
        estimatedPeakLabel.setText(currentBuild.getEstimatedPeakPowerWatts() + " W");
        recommendedPsuLabel.setText(currentBuild.getRecommendedPsuWatts() + " W (+20% Headroom)");

        // Compatibility Evaluation
        CompatibilityResult result = compatibilityChecker.checkCompatibility(currentBuild);
        updateCompatibilityDisplay(result);
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
