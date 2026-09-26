package com.pcanalyzer.service;

import com.pcanalyzer.model.Cpu;
import com.pcanalyzer.model.Gpu;

// Calculates price-to-performance metrics like score per dollar and frames per dollar
public class BuildAnalyzer {

    // Calculate how many dollars each CPU benchmark point costs (lower is better)
    public double calculateCpuCostPerPoint(Cpu cpu) {
        if (cpu == null || cpu.getBenchmarkScore() <= 0) return 0.0;
        return cpu.getPrice() / cpu.getBenchmarkScore();
    }

    // Calculate how many benchmark points you get per dollar spent (higher is better)
    public double calculateCpuPointsPerDollar(Cpu cpu) {
        if (cpu == null || cpu.getPrice() <= 0) return 0.0;
        return cpu.getBenchmarkScore() / cpu.getPrice();
    }

    // Calculate how much each gaming frame per second costs in dollars (lower is better)
    public double calculateGpuCostPerFps(Gpu gpu, String resolution) {
        if (gpu == null) return 0.0;
        double fps = gpu.getFpsForResolution(resolution);
        if (fps <= 0) return 0.0;
        return gpu.getPrice() / fps;
    }

    // Calculate how many gaming frames per second you get for each dollar (higher is better)
    public double calculateGpuFpsPerDollar(Gpu gpu, String resolution) {
        if (gpu == null || gpu.getPrice() <= 0) return 0.0;
        double fps = gpu.getFpsForResolution(resolution);
        return fps / gpu.getPrice();
    }
}
