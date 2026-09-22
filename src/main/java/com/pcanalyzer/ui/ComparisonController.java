package com.pcanalyzer.ui;

import com.pcanalyzer.db.ComponentDao;
import com.pcanalyzer.model.*;
import com.pcanalyzer.service.BuildAnalyzer;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.*;

import java.util.List;

/**
 * Controller for the Performance Comparisons tab (Phase 5).
 *
 * Design Decision:
 * 1. Thin Controller: Delegates value calculations to BuildAnalyzer and data retrieval
 *    to ComponentDao. This controller only transforms domain data into chart series.
 * 2. Reactive Updates: Switching category (GPU/CPU) or resolution triggers a full
 *    rebuild of all chart series and the head-to-head selectors.
 */
public class ComparisonController {

    // Toolbar controls
    @FXML private ComboBox<String> categoryComboBox;
    @FXML private ComboBox<String> resolutionComboBox;
    @FXML private Label resolutionLabel;

    // Main benchmark chart
    @FXML private BarChart<String, Number> mainBarChart;
    @FXML private CategoryAxis mainXAxis;
    @FXML private NumberAxis mainYAxis;
    @FXML private Label mainChartTitle;

    // Price-to-performance chart
    @FXML private BarChart<String, Number> valueBarChart;
    @FXML private CategoryAxis valueXAxis;
    @FXML private NumberAxis valueYAxis;
    @FXML private Label valueChartTitle;

    // Head-to-head comparison
    @FXML private ComboBox<Component> compareComboA;
    @FXML private ComboBox<Component> compareComboB;
    @FXML private GridPane comparisonGrid;

    private ComponentDao componentDao;
    private final BuildAnalyzer buildAnalyzer = new BuildAnalyzer();

    @FXML
    public void initialize() {
        // Category selector
        categoryComboBox.setItems(FXCollections.observableArrayList("GPU (Graphics Card)", "CPU (Processor)"));
        categoryComboBox.getSelectionModel().selectFirst();
        categoryComboBox.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> onCategoryChanged());

        // Resolution selector (GPU-only)
        resolutionComboBox.setItems(FXCollections.observableArrayList("1080p (FHD)", "1440p (QHD)", "4K (UHD)"));
        resolutionComboBox.getSelectionModel().selectFirst();
        resolutionComboBox.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> refreshCharts());

        // Head-to-head selection listeners
        compareComboA.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> updateHeadToHead());
        compareComboB.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> updateHeadToHead());
    }

    /**
     * Receives the DAO from MainController.
     */
    public void setComponentDao(ComponentDao componentDao) {
        this.componentDao = componentDao;
        refreshCharts();
    }

    @FXML
    private void handleRefresh() {
        refreshCharts();
    }

    private void onCategoryChanged() {
        String category = categoryComboBox.getSelectionModel().getSelectedItem();
        boolean isGpu = category != null && category.startsWith("GPU");
        resolutionComboBox.setVisible(isGpu);
        resolutionComboBox.setManaged(isGpu);
        resolutionLabel.setVisible(isGpu);
        resolutionLabel.setManaged(isGpu);
        refreshCharts();
    }

    /**
     * Rebuilds all chart series and head-to-head selectors based on the current
     * category and resolution selection.
     */
    private void refreshCharts() {
        if (componentDao == null) return;

        String category = categoryComboBox.getSelectionModel().getSelectedItem();
        if (category == null) return;

        if (category.startsWith("GPU")) {
            List<Gpu> gpus = componentDao.findByType(ComponentType.GPU).stream()
                    .map(c -> (Gpu) c)
                    .toList();
            buildGpuFpsChart(gpus);
            buildGpuValueChart(gpus);
            populateHeadToHeadSelectors(componentDao.findByType(ComponentType.GPU));
        } else {
            List<Cpu> cpus = componentDao.findByType(ComponentType.CPU).stream()
                    .map(c -> (Cpu) c)
                    .toList();
            buildCpuBenchmarkChart(cpus);
            buildCpuValueChart(cpus);
            populateHeadToHeadSelectors(componentDao.findByType(ComponentType.CPU));
        }
    }

    // =====================================================================
    // GPU Charts
    // =====================================================================

    private void buildGpuFpsChart(List<Gpu> gpus) {
        mainBarChart.getData().clear();
        mainChartTitle.setText("GPU Gaming FPS Comparison");
        mainXAxis.setLabel("GPU");
        mainYAxis.setLabel("Average FPS");

        XYChart.Series<String, Number> series1080 = new XYChart.Series<>();
        series1080.setName("1080p (FHD)");
        XYChart.Series<String, Number> series1440 = new XYChart.Series<>();
        series1440.setName("1440p (QHD)");
        XYChart.Series<String, Number> series4k = new XYChart.Series<>();
        series4k.setName("4K (UHD)");

        for (Gpu gpu : gpus) {
            String label = gpu.getBrand() + " " + gpu.getName();
            series1080.getData().add(new XYChart.Data<>(label, gpu.getFps1080p()));
            series1440.getData().add(new XYChart.Data<>(label, gpu.getFps1440p()));
            series4k.getData().add(new XYChart.Data<>(label, gpu.getFps4k()));
        }

        mainBarChart.getData().addAll(series1080, series1440, series4k);
    }

    private void buildGpuValueChart(List<Gpu> gpus) {
        valueBarChart.getData().clear();
        String resolution = getSelectedResolutionKey();
        String resLabel = resolutionComboBox.getSelectionModel().getSelectedItem();
        if (resLabel == null) resLabel = "1080p";

        valueChartTitle.setText("Price-to-Performance: $/FPS at " + resLabel);
        valueXAxis.setLabel("GPU");
        valueYAxis.setLabel("$/FPS (lower is better)");

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("$/FPS");

        for (Gpu gpu : gpus) {
            String label = gpu.getBrand() + " " + gpu.getName();
            double costPerFps = buildAnalyzer.calculateGpuCostPerFps(gpu, resolution);
            if (costPerFps > 0) {
                series.getData().add(new XYChart.Data<>(label, costPerFps));
            }
        }

        valueBarChart.getData().add(series);
    }

    // =====================================================================
    // CPU Charts
    // =====================================================================

    private void buildCpuBenchmarkChart(List<Cpu> cpus) {
        mainBarChart.getData().clear();
        mainChartTitle.setText("CPU Benchmark Score Comparison");
        mainXAxis.setLabel("CPU");
        mainYAxis.setLabel("Benchmark Score");

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Benchmark Score");

        for (Cpu cpu : cpus) {
            String label = cpu.getBrand() + " " + cpu.getName();
            series.getData().add(new XYChart.Data<>(label, cpu.getBenchmarkScore()));
        }

        mainBarChart.getData().add(series);
    }

    private void buildCpuValueChart(List<Cpu> cpus) {
        valueBarChart.getData().clear();
        valueChartTitle.setText("Price-to-Performance: Points/$ (higher is better)");
        valueXAxis.setLabel("CPU");
        valueYAxis.setLabel("Points/$ (higher is better)");

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Points/$");

        for (Cpu cpu : cpus) {
            String label = cpu.getBrand() + " " + cpu.getName();
            double pointsPerDollar = buildAnalyzer.calculateCpuPointsPerDollar(cpu);
            if (pointsPerDollar > 0) {
                series.getData().add(new XYChart.Data<>(label, pointsPerDollar));
            }
        }

        valueBarChart.getData().add(series);
    }

    // =====================================================================
    // Head-to-Head Comparison
    // =====================================================================

    private void populateHeadToHeadSelectors(List<Component> components) {
        ObservableList<Component> items = FXCollections.observableArrayList(components);

        Component prevA = compareComboA.getSelectionModel().getSelectedItem();
        Component prevB = compareComboB.getSelectionModel().getSelectedItem();

        compareComboA.setItems(items);
        compareComboB.setItems(items);

        // Restore previous selections if still valid
        if (prevA != null && items.contains(prevA)) {
            compareComboA.getSelectionModel().select(prevA);
        } else if (!items.isEmpty()) {
            compareComboA.getSelectionModel().select(0);
        }

        if (prevB != null && items.contains(prevB)) {
            compareComboB.getSelectionModel().select(prevB);
        } else if (items.size() > 1) {
            compareComboB.getSelectionModel().select(1);
        } else if (!items.isEmpty()) {
            compareComboB.getSelectionModel().select(0);
        }
    }

    private void updateHeadToHead() {
        comparisonGrid.getChildren().clear();
        comparisonGrid.getColumnConstraints().clear();

        Component a = compareComboA.getSelectionModel().getSelectedItem();
        Component b = compareComboB.getSelectionModel().getSelectedItem();
        if (a == null || b == null) return;

        // Column constraints: Spec | Value A | Value B
        ColumnConstraints specCol = new ColumnConstraints();
        specCol.setPercentWidth(40);
        ColumnConstraints valACol = new ColumnConstraints();
        valACol.setPercentWidth(30);
        valACol.setHalignment(HPos.CENTER);
        ColumnConstraints valBCol = new ColumnConstraints();
        valBCol.setPercentWidth(30);
        valBCol.setHalignment(HPos.CENTER);
        comparisonGrid.getColumnConstraints().addAll(specCol, valACol, valBCol);

        // Header row
        addComparisonHeader(0, "Specification", a.getBrand() + " " + a.getName(), b.getBrand() + " " + b.getName());

        int row = 1;
        // Common fields
        row = addComparisonRow(row, "Price", String.format("$%.2f", a.getPrice()), String.format("$%.2f", b.getPrice()), a.getPrice(), b.getPrice(), true);
        row = addComparisonRow(row, "TDP", a.getTdpWatts() + " W", b.getTdpWatts() + " W", a.getTdpWatts(), b.getTdpWatts(), true);

        // Type-specific fields
        if (a instanceof Cpu cpuA && b instanceof Cpu cpuB) {
            row = addComparisonRow(row, "Socket", cpuA.getSocket(), cpuB.getSocket(), 0, 0, false);
            row = addComparisonRow(row, "Cores", String.valueOf(cpuA.getCores()), String.valueOf(cpuB.getCores()), cpuA.getCores(), cpuB.getCores(), false);
            row = addComparisonRow(row, "Threads", String.valueOf(cpuA.getThreads()), String.valueOf(cpuB.getThreads()), cpuA.getThreads(), cpuB.getThreads(), false);
            row = addComparisonRow(row, "Base Clock", String.format("%.1f GHz", cpuA.getBaseClockGhz()), String.format("%.1f GHz", cpuB.getBaseClockGhz()), cpuA.getBaseClockGhz(), cpuB.getBaseClockGhz(), false);
            row = addComparisonRow(row, "Boost Clock", String.format("%.1f GHz", cpuA.getBoostClockGhz()), String.format("%.1f GHz", cpuB.getBoostClockGhz()), cpuA.getBoostClockGhz(), cpuB.getBoostClockGhz(), false);
            row = addComparisonRow(row, "Benchmark", String.format("%,d", cpuA.getBenchmarkScore()), String.format("%,d", cpuB.getBenchmarkScore()), cpuA.getBenchmarkScore(), cpuB.getBenchmarkScore(), false);
            double ppdA = buildAnalyzer.calculateCpuPointsPerDollar(cpuA);
            double ppdB = buildAnalyzer.calculateCpuPointsPerDollar(cpuB);
            row = addComparisonRow(row, "Points/$", String.format("%.1f", ppdA), String.format("%.1f", ppdB), ppdA, ppdB, false);
        } else if (a instanceof Gpu gpuA && b instanceof Gpu gpuB) {
            row = addComparisonRow(row, "VRAM", gpuA.getVramGb() + " GB", gpuB.getVramGb() + " GB", gpuA.getVramGb(), gpuB.getVramGb(), false);
            row = addComparisonRow(row, "Board Power", gpuA.getBoardPowerWatts() + " W", gpuB.getBoardPowerWatts() + " W", gpuA.getBoardPowerWatts(), gpuB.getBoardPowerWatts(), true);
            row = addComparisonRow(row, "FPS 1080p", String.format("%.1f", gpuA.getFps1080p()), String.format("%.1f", gpuB.getFps1080p()), gpuA.getFps1080p(), gpuB.getFps1080p(), false);
            row = addComparisonRow(row, "FPS 1440p", String.format("%.1f", gpuA.getFps1440p()), String.format("%.1f", gpuB.getFps1440p()), gpuA.getFps1440p(), gpuB.getFps1440p(), false);
            row = addComparisonRow(row, "FPS 4K", String.format("%.1f", gpuA.getFps4k()), String.format("%.1f", gpuB.getFps4k()), gpuA.getFps4k(), gpuB.getFps4k(), false);
            double cpfA = buildAnalyzer.calculateGpuCostPerFps(gpuA, "1080p");
            double cpfB = buildAnalyzer.calculateGpuCostPerFps(gpuB, "1080p");
            row = addComparisonRow(row, "$/FPS (1080p)", String.format("$%.2f", cpfA), String.format("$%.2f", cpfB), cpfA, cpfB, true);
            row = addComparisonRow(row, "Rec. PSU", gpuA.getRecommendedPsuWatts() + " W", gpuB.getRecommendedPsuWatts() + " W", gpuA.getRecommendedPsuWatts(), gpuB.getRecommendedPsuWatts(), true);
        }
    }

    /**
     * Adds a header row to the comparison grid.
     */
    private void addComparisonHeader(int row, String specText, String nameA, String nameB) {
        Label specLabel = new Label(specText);
        specLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #ffffff;");
        Label labelA = new Label(nameA);
        labelA.setStyle("-fx-font-weight: bold; -fx-text-fill: #ffffff;");
        labelA.setWrapText(true);
        Label labelB = new Label(nameB);
        labelB.setStyle("-fx-font-weight: bold; -fx-text-fill: #ffffff;");
        labelB.setWrapText(true);

        HBox headerBox = new HBox();
        headerBox.setStyle("-fx-background-color: #2b579a; -fx-padding: 4px 6px;");
        GridPane.setColumnSpan(headerBox, 3);

        // Build a sub-grid inside the header
        GridPane headerGrid = new GridPane();
        headerGrid.setHgap(6);
        ColumnConstraints c1 = new ColumnConstraints();
        c1.setPercentWidth(40);
        ColumnConstraints c2 = new ColumnConstraints();
        c2.setPercentWidth(30);
        c2.setHalignment(HPos.CENTER);
        ColumnConstraints c3 = new ColumnConstraints();
        c3.setPercentWidth(30);
        c3.setHalignment(HPos.CENTER);
        headerGrid.getColumnConstraints().addAll(c1, c2, c3);
        headerGrid.add(specLabel, 0, 0);
        headerGrid.add(labelA, 1, 0);
        headerGrid.add(labelB, 2, 0);
        headerGrid.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(headerGrid, Priority.ALWAYS);

        headerBox.getChildren().add(headerGrid);
        comparisonGrid.add(headerBox, 0, row, 3, 1);
    }

    /**
     * Adds a data row to the comparison grid with color-coded advantage indicators.
     *
     * @param lowerIsBetter If true, the lower numeric value is highlighted as better (e.g. price, power).
     *                      If false, the higher value is better (e.g. benchmark score, FPS).
     */
    private int addComparisonRow(int row, String specName, String valueA, String valueB,
                                  double numA, double numB, boolean lowerIsBetter) {
        Label specLabel = new Label(specName);
        specLabel.setStyle("-fx-font-weight: bold;");
        specLabel.setPadding(new Insets(2, 6, 2, 6));

        Label labelA = new Label(valueA);
        labelA.setPadding(new Insets(2, 6, 2, 6));
        labelA.setMaxWidth(Double.MAX_VALUE);

        Label labelB = new Label(valueB);
        labelB.setPadding(new Insets(2, 6, 2, 6));
        labelB.setMaxWidth(Double.MAX_VALUE);

        // Color code: green for winner, red for loser (only if values differ)
        if (numA != numB && numA != 0 && numB != 0) {
            boolean aWins;
            if (lowerIsBetter) {
                aWins = numA < numB;
            } else {
                aWins = numA > numB;
            }

            if (aWins) {
                labelA.setStyle(labelA.getStyle() + "-fx-background-color: #d4edda;");
                labelB.setStyle(labelB.getStyle() + "-fx-background-color: #f8d7da;");
            } else {
                labelA.setStyle(labelA.getStyle() + "-fx-background-color: #f8d7da;");
                labelB.setStyle(labelB.getStyle() + "-fx-background-color: #d4edda;");
            }
        }

        // Alternating row background
        String rowBg = (row % 2 == 0) ? "-fx-background-color: #f4f4f4;" : "-fx-background-color: #ffffff;";
        specLabel.setStyle(specLabel.getStyle() + rowBg);

        comparisonGrid.add(specLabel, 0, row);
        comparisonGrid.add(labelA, 1, row);
        comparisonGrid.add(labelB, 2, row);

        return row + 1;
    }

    // =====================================================================
    // Helpers
    // =====================================================================

    /**
     * Converts the resolution combo selection to a key string for BuildAnalyzer.
     */
    private String getSelectedResolutionKey() {
        String selected = resolutionComboBox.getSelectionModel().getSelectedItem();
        if (selected == null) return "1080p";
        if (selected.contains("1080")) return "1080p";
        if (selected.contains("1440")) return "1440p";
        if (selected.contains("4K")) return "4K";
        return "1080p";
    }
}
