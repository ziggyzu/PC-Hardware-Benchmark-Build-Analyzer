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
import javafx.scene.control.SplitPane;
import javafx.scene.layout.*;

import java.util.List;

// Controls the comparison tab where users compare speeds, frame rates, and value
public class ComparisonController {

    // Main layout panes
    @FXML private VBox comparisonRoot;
    @FXML private SplitPane verticalSplitPane;
    @FXML private SplitPane bottomSplitPane;

    // Filter selectors
    @FXML private ComboBox<String> categoryComboBox;
    @FXML private ComboBox<String> resolutionComboBox;
    @FXML private Label resolutionLabel;

    // Primary speed comparison chart
    @FXML private BarChart<String, Number> mainBarChart;
    @FXML private CategoryAxis mainXAxis;
    @FXML private NumberAxis mainYAxis;

    // Price to performance chart
    @FXML private BarChart<String, Number> valueBarChart;
    @FXML private CategoryAxis valueXAxis;
    @FXML private NumberAxis valueYAxis;

    // Side-by-side comparison selectors and grid
    @FXML private ComboBox<Component> compareComboA;
    @FXML private ComboBox<Component> compareComboB;
    @FXML private GridPane comparisonGrid;

    // Helper tools
    private ComponentDao componentDao;
    private final BuildAnalyzer buildAnalyzer = new BuildAnalyzer();

    // Set up responsive chart heights, dropdown items, and listeners
    @FXML
    public void initialize() {
        // Keep charts nicely proportioned when resizing the window
        mainBarChart.minHeightProperty().bind(verticalSplitPane.heightProperty().multiply(0.35));
        valueBarChart.minHeightProperty().bind(bottomSplitPane.heightProperty().multiply(0.40));

        // Choose between graphics cards or processors
        categoryComboBox.setItems(FXCollections.observableArrayList("GPU (Graphics Card)", "CPU (Processor)"));
        categoryComboBox.getSelectionModel().selectFirst();
        categoryComboBox.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> onCategoryChanged());

        // Choose resolution for gaming benchmarks
        resolutionComboBox.setItems(FXCollections.observableArrayList("1080p (FHD)", "1440p (QHD)", "4K (UHD)"));
        resolutionComboBox.getSelectionModel().selectFirst();
        resolutionComboBox.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> refreshCharts());

        // Update the head-to-head comparison table whenever a new part is chosen
        compareComboA.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> updateHeadToHead());
        compareComboB.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> updateHeadToHead());
    }

    // Connect the database and draw the initial charts
    public void setComponentDao(ComponentDao componentDao) {
        this.componentDao = componentDao;
        refreshCharts();
    }

    // Redraw all charts
    @FXML
    public void handleRefresh() {
        refreshCharts();
    }

    // Show or hide the resolution dropdown based on whether GPUs or CPUs are selected
    private void onCategoryChanged() {
        String category = categoryComboBox.getSelectionModel().getSelectedItem();
        boolean isGpu = category != null && category.startsWith("GPU");
        resolutionComboBox.setVisible(isGpu);
        resolutionComboBox.setManaged(isGpu);
        resolutionLabel.setVisible(isGpu);
        resolutionLabel.setManaged(isGpu);
        refreshCharts();
    }

    // Grab data in the background and redraw all charts
    private void refreshCharts() {
        if (componentDao == null) return;

        String category = categoryComboBox.getSelectionModel().getSelectedItem();
        if (category == null) return;

        boolean isGpu = category.startsWith("GPU");

        // Load the parts list on a background thread so the window doesn't freeze
        javafx.concurrent.Task<List<Component>> chartDataTask = new javafx.concurrent.Task<>() {
            @Override
            protected List<Component> call() {
                ComponentType type = isGpu ? ComponentType.GPU : ComponentType.CPU;
                return componentDao.findByType(type);
            }
        };

        // When the query finishes, update both charts on the screen
        chartDataTask.setOnSucceeded(event -> {
            List<Component> components = chartDataTask.getValue();
            if (isGpu) {
                List<Gpu> gpus = components.stream().map(c -> (Gpu) c).toList();
                buildGpuFpsChart(gpus);
                buildGpuValueChart(gpus);
            } else {
                List<Cpu> cpus = components.stream().map(c -> (Cpu) c).toList();
                buildCpuBenchmarkChart(cpus);
                buildCpuValueChart(cpus);
            }
            populateHeadToHeadSelectors(components);
        });

        com.pcanalyzer.util.ThreadPoolManager.getInstance().submitTask(chartDataTask);
    }

    // Draw the GPU gaming frame rate bar chart
    private void buildGpuFpsChart(List<Gpu> gpus) {
        mainBarChart.getData().clear();
        mainBarChart.setTitle("GPU Gaming FPS Comparison (Multi-Resolution)");
        mainXAxis.setLabel("GPU Model");
        mainYAxis.setLabel("Average Frames Per Second (FPS)");

        // Create bars for 1080p, 1440p, and 4K
        XYChart.Series<String, Number> series1080 = new XYChart.Series<>();
        series1080.setName("1080p (FHD)");
        XYChart.Series<String, Number> series1440 = new XYChart.Series<>();
        series1440.setName("1440p (QHD)");
        XYChart.Series<String, Number> series4k = new XYChart.Series<>();
        series4k.setName("4K (UHD)");

        // Add each graphics card to the chart
        for (Gpu gpu : gpus) {
            String label = gpu.getBrand() + " " + gpu.getName();
            series1080.getData().add(new XYChart.Data<>(label, gpu.getFps1080p()));
            series1440.getData().add(new XYChart.Data<>(label, gpu.getFps1440p()));
            series4k.getData().add(new XYChart.Data<>(label, gpu.getFps4k()));
        }

        mainBarChart.getData().addAll(series1080, series1440, series4k);
    }

    // Draw the GPU price-to-performance value chart
    private void buildGpuValueChart(List<Gpu> gpus) {
        valueBarChart.getData().clear();
        String resolution = getSelectedResolutionKey();
        String resLabel = resolutionComboBox.getSelectionModel().getSelectedItem();
        if (resLabel == null) resLabel = "1080p";

        valueBarChart.setTitle("Price-to-Performance ($/FPS at " + resLabel + " — Lower is Better)");
        valueXAxis.setLabel("GPU Model");
        valueYAxis.setLabel("USD per FPS");

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("$/FPS");

        // Calculate and add cost per frame for each GPU
        for (Gpu gpu : gpus) {
            String label = gpu.getBrand() + " " + gpu.getName();
            double costPerFps = buildAnalyzer.calculateGpuCostPerFps(gpu, resolution);
            if (costPerFps > 0) {
                series.getData().add(new XYChart.Data<>(label, costPerFps));
            }
        }

        valueBarChart.getData().add(series);
    }

    // Draw the processor benchmark score chart
    private void buildCpuBenchmarkChart(List<Cpu> cpus) {
        mainBarChart.getData().clear();
        mainBarChart.setTitle("CPU Multi-Threaded Benchmark Score (Higher is Better)");
        mainXAxis.setLabel("CPU Model");
        mainYAxis.setLabel("Benchmark Score");

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Benchmark Score");

        // Add benchmark scores for each CPU
        for (Cpu cpu : cpus) {
            String label = cpu.getBrand() + " " + cpu.getName();
            series.getData().add(new XYChart.Data<>(label, cpu.getBenchmarkScore()));
        }

        mainBarChart.getData().add(series);
    }

    // Draw the CPU value chart showing points per dollar
    private void buildCpuValueChart(List<Cpu> cpus) {
        valueBarChart.getData().clear();
        valueBarChart.setTitle("CPU Value Efficiency (Points per Dollar — Higher is Better)");
        valueXAxis.setLabel("CPU Model");
        valueYAxis.setLabel("Points / USD");

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Points/$");

        // Calculate points per dollar for each CPU
        for (Cpu cpu : cpus) {
            String label = cpu.getBrand() + " " + cpu.getName();
            double pointsPerDollar = buildAnalyzer.calculateCpuPointsPerDollar(cpu);
            if (pointsPerDollar > 0) {
                series.getData().add(new XYChart.Data<>(label, pointsPerDollar));
            }
        }

        valueBarChart.getData().add(series);
    }

    // Fill the two head-to-head comparison dropdowns
    private void populateHeadToHeadSelectors(List<Component> components) {
        ObservableList<Component> items = FXCollections.observableArrayList(components);

        Component prevA = compareComboA.getSelectionModel().getSelectedItem();
        Component prevB = compareComboB.getSelectionModel().getSelectedItem();

        compareComboA.setItems(items);
        compareComboB.setItems(items);

        // Keep previous selections if they're still in the list
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

    // Build the side-by-side comparison table
    private void updateHeadToHead() {
        comparisonGrid.getChildren().clear();
        comparisonGrid.getColumnConstraints().clear();

        Component a = compareComboA.getSelectionModel().getSelectedItem();
        Component b = compareComboB.getSelectionModel().getSelectedItem();
        if (a == null || b == null) return;

        // Size the columns proportionally
        ColumnConstraints specCol = new ColumnConstraints();
        specCol.setPercentWidth(40);
        ColumnConstraints valACol = new ColumnConstraints();
        valACol.setPercentWidth(30);
        valACol.setHalignment(HPos.CENTER);
        ColumnConstraints valBCol = new ColumnConstraints();
        valBCol.setPercentWidth(30);
        valBCol.setHalignment(HPos.CENTER);
        comparisonGrid.getColumnConstraints().addAll(specCol, valACol, valBCol);

        // Add the table header
        addComparisonHeader(0, "Specification", a.getBrand() + " " + a.getName(), b.getBrand() + " " + b.getName());

        int row = 1;
        // Compare price and wattage
        row = addComparisonRow(row, "Price", String.format("$%.2f", a.getPrice()), String.format("$%.2f", b.getPrice()), a.getPrice(), b.getPrice(), true);
        row = addComparisonRow(row, "TDP", a.getTdpWatts() + " W", b.getTdpWatts() + " W", a.getTdpWatts(), b.getTdpWatts(), true);

        // Compare CPU-specific specs
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
            // Compare GPU-specific specs
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

    // Add a title header to the comparison grid
    private void addComparisonHeader(int row, String specText, String nameA, String nameB) {
        Label specLabel = new Label(specText);
        specLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #1e3a8a;");
        Label labelA = new Label(nameA);
        labelA.setStyle("-fx-font-weight: bold; -fx-text-fill: #1e3a8a;");
        labelA.setWrapText(true);
        Label labelB = new Label(nameB);
        labelB.setStyle("-fx-font-weight: bold; -fx-text-fill: #1e3a8a;");
        labelB.setWrapText(true);

        comparisonGrid.add(specLabel, 0, row);
        comparisonGrid.add(labelA, 1, row);
        comparisonGrid.add(labelB, 2, row);
    }

    // Add a comparison row and highlight the better value in green
    private int addComparisonRow(int row, String specName, String valueA, String valueB,
                                  double numA, double numB, boolean lowerIsBetter) {
        Label specLabel = new Label(specName);
        specLabel.setStyle("-fx-font-weight: bold;");
        specLabel.setPadding(new Insets(2, 4, 2, 4));

        Label labelA = new Label(valueA);
        labelA.setPadding(new Insets(2, 4, 2, 4));
        labelA.setMaxWidth(Double.MAX_VALUE);

        Label labelB = new Label(valueB);
        labelB.setPadding(new Insets(2, 4, 2, 4));
        labelB.setMaxWidth(Double.MAX_VALUE);

        // Highlight the winner in green and the loser in red
        if (numA != numB && numA != 0 && numB != 0) {
            boolean aWins = lowerIsBetter ? (numA < numB) : (numA > numB);
            if (aWins) {
                labelA.setStyle("-fx-background-color: #dcfce7; -fx-text-fill: #15803d; -fx-font-weight: bold; -fx-background-radius: 3px;");
                labelB.setStyle("-fx-background-color: #fee2e2; -fx-text-fill: #b91c1c; -fx-background-radius: 3px;");
            } else {
                labelA.setStyle("-fx-background-color: #fee2e2; -fx-text-fill: #b91c1c; -fx-background-radius: 3px;");
                labelB.setStyle("-fx-background-color: #dcfce7; -fx-text-fill: #15803d; -fx-font-weight: bold; -fx-background-radius: 3px;");
            }
        }

        comparisonGrid.add(specLabel, 0, row);
        comparisonGrid.add(labelA, 1, row);
        comparisonGrid.add(labelB, 2, row);

        return row + 1;
    }

    // Convert the resolution dropdown text into a key for lookup
    private String getSelectedResolutionKey() {
        String selected = resolutionComboBox.getSelectionModel().getSelectedItem();
        if (selected == null) return "1080p";
        if (selected.contains("1080")) return "1080p";
        if (selected.contains("1440")) return "1440p";
        if (selected.contains("4K")) return "4K";
        return "1080p";
    }
}
