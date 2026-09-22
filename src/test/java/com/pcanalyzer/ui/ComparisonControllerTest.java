package com.pcanalyzer.ui;

import com.pcanalyzer.model.Cpu;
import com.pcanalyzer.model.Gpu;
import com.pcanalyzer.service.BuildAnalyzer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the data logic used by ComparisonController.
 * These tests verify chart data preparation and value calculations
 * without requiring a JavaFX runtime.
 */
class ComparisonControllerTest {

    private BuildAnalyzer analyzer;

    @BeforeEach
    void setUp() {
        analyzer = new BuildAnalyzer();
    }

    @Test
    @DisplayName("GPU cost-per-FPS at 1080p calculated correctly")
    void testGpuCostPerFps1080p() {
        Gpu gpu = new Gpu(1, "NVIDIA", "RTX 4060", 299.99, 115, 8, 115, 500, 96.5, 66.0, 34.0);

        double costPerFps = analyzer.calculateGpuCostPerFps(gpu, "1080p");
        // $299.99 / 96.5 FPS ≈ $3.11
        assertTrue(costPerFps > 3.0 && costPerFps < 3.2, "Cost per FPS should be ~$3.11");
    }

    @Test
    @DisplayName("GPU cost-per-FPS at 4K calculated correctly")
    void testGpuCostPerFps4K() {
        Gpu gpu = new Gpu(1, "NVIDIA", "RTX 4060", 299.99, 115, 8, 115, 500, 96.5, 66.0, 34.0);

        double costPerFps = analyzer.calculateGpuCostPerFps(gpu, "4K");
        // $299.99 / 34.0 FPS ≈ $8.82
        assertTrue(costPerFps > 8.5 && costPerFps < 9.0, "Cost per FPS at 4K should be ~$8.82");
    }

    @Test
    @DisplayName("GPU FPS-per-dollar at 1080p calculated correctly")
    void testGpuFpsPerDollar() {
        Gpu gpu = new Gpu(1, "NVIDIA", "RTX 4060", 299.99, 115, 8, 115, 500, 96.5, 66.0, 34.0);

        double fpsPerDollar = analyzer.calculateGpuFpsPerDollar(gpu, "1080p");
        // 96.5 / $299.99 ≈ 0.322
        assertTrue(fpsPerDollar > 0.3 && fpsPerDollar < 0.35, "FPS per dollar should be ~0.322");
    }

    @Test
    @DisplayName("CPU points-per-dollar calculated correctly")
    void testCpuPointsPerDollar() {
        Cpu cpu = new Cpu(1, "AMD", "Ryzen 5 7600", 189.99, 65, "AM5", 6, 12, 3.8, 5.1, 27500);

        double pointsPerDollar = analyzer.calculateCpuPointsPerDollar(cpu);
        // 27500 / $189.99 ≈ 144.7
        assertTrue(pointsPerDollar > 140 && pointsPerDollar < 150, "Points per dollar should be ~144.7");
    }

    @Test
    @DisplayName("CPU cost-per-point calculated correctly")
    void testCpuCostPerPoint() {
        Cpu cpu = new Cpu(1, "AMD", "Ryzen 5 7600", 189.99, 65, "AM5", 6, 12, 3.8, 5.1, 27500);

        double costPerPoint = analyzer.calculateCpuCostPerPoint(cpu);
        // $189.99 / 27500 ≈ $0.0069
        assertTrue(costPerPoint > 0.006 && costPerPoint < 0.008, "Cost per point should be ~$0.0069");
    }

    @Test
    @DisplayName("Value comparison identifies better GPU correctly")
    void testGpuValueComparison() {
        Gpu budget = new Gpu(1, "NVIDIA", "RTX 4060", 299.99, 115, 8, 115, 500, 96.5, 66.0, 34.0);
        Gpu highEnd = new Gpu(2, "NVIDIA", "RTX 4070 Ti", 799.99, 285, 12, 285, 700, 170.0, 130.0, 80.0);

        double budgetCpf = analyzer.calculateGpuCostPerFps(budget, "1080p");
        double highEndCpf = analyzer.calculateGpuCostPerFps(highEnd, "1080p");

        // Budget GPU should have lower $/FPS (better value)
        assertTrue(budgetCpf < highEndCpf, "Budget GPU should have better cost per FPS");
    }

    @Test
    @DisplayName("Value comparison identifies better CPU correctly")
    void testCpuValueComparison() {
        Cpu budget = new Cpu(1, "AMD", "Ryzen 5 5500", 89.99, 65, "AM4", 6, 12, 3.6, 4.2, 19500);
        Cpu midRange = new Cpu(2, "AMD", "Ryzen 5 7600", 189.99, 65, "AM5", 6, 12, 3.8, 5.1, 27500);

        double budgetPpd = analyzer.calculateCpuPointsPerDollar(budget);
        double midRangePpd = analyzer.calculateCpuPointsPerDollar(midRange);

        // Budget CPU should have higher Points/$ (better value)
        assertTrue(budgetPpd > midRangePpd, "Budget CPU should have better points per dollar");
    }

    @Test
    @DisplayName("Zero-price GPU returns zero for FPS-per-dollar")
    void testZeroPriceGpu() {
        Gpu gpu = new Gpu(1, "Test", "Free GPU", 0.0, 100, 8, 100, 500, 60.0, 40.0, 20.0);
        assertEquals(0.0, analyzer.calculateGpuFpsPerDollar(gpu, "1080p"));
    }

    @Test
    @DisplayName("Zero-score CPU returns zero for cost-per-point")
    void testZeroScoreCpu() {
        Cpu cpu = new Cpu(1, "Test", "No Score", 100.0, 65, "AM5", 4, 8, 3.0, 4.0, 0);
        assertEquals(0.0, analyzer.calculateCpuCostPerPoint(cpu));
    }
}
