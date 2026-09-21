package com.pcanalyzer.service;

import com.pcanalyzer.model.Cpu;
import com.pcanalyzer.model.Gpu;

/**
 * Service providing price-to-performance calculations and benchmark analysis.
 *
 * Design Decision:
 * Isolates numerical analysis algorithms into a dedicated service.
 * Allows comparing components objectively (e.g. Dollar-per-FPS, Benchmark-per-Dollar).
 */
public class BuildAnalyzer {

    /**
     * Calculate cost per benchmark point for a CPU ($ / point). Lower is better value.
     */
    public double calculateCpuCostPerPoint(Cpu cpu) {
        if (cpu == null || cpu.getBenchmarkScore() <= 0) return 0.0;
        return cpu.getPrice() / cpu.getBenchmarkScore();
    }

    /**
     * Calculate benchmark points per dollar for a CPU (points / $). Higher is better value.
     */
    public double calculateCpuPointsPerDollar(Cpu cpu) {
        if (cpu == null || cpu.getPrice() <= 0) return 0.0;
        return cpu.getBenchmarkScore() / cpu.getPrice();
    }

    /**
     * Calculate cost per frame for a GPU at a specified resolution ($ / FPS). Lower is better value.
     */
    public double calculateGpuCostPerFps(Gpu gpu, String resolution) {
        if (gpu == null) return 0.0;
        double fps = gpu.getFpsForResolution(resolution);
        if (fps <= 0) return 0.0;
        return gpu.getPrice() / fps;
    }

    /**
     * Calculate frames per dollar for a GPU (FPS / $). Higher is better value.
     */
    public double calculateGpuFpsPerDollar(Gpu gpu, String resolution) {
        if (gpu == null || gpu.getPrice() <= 0) return 0.0;
        double fps = gpu.getFpsForResolution(resolution);
        return fps / gpu.getPrice();
    }
}
