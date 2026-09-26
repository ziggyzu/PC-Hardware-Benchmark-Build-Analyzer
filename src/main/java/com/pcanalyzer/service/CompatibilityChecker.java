package com.pcanalyzer.service;

import com.pcanalyzer.model.*;

import java.util.ArrayList;
import java.util.List;

// Checks whether components in a build will physically and electrically work together
public class CompatibilityChecker {

    // Run all checks on the build and collect any warnings or errors
    public CompatibilityResult checkCompatibility(Build build) {
        // If there is no build, treat it as compatible by default
        if (build == null) {
            return CompatibilityResult.compatible();
        }

        List<CompatibilityIssue> issues = new ArrayList<>();
        CompatibilityStatus worstStatus = CompatibilityStatus.COMPATIBLE;

        // Check if CPU socket matches the motherboard socket
        if (build.getCpu() != null && build.getMotherboard() != null) {
            String cpuSocket = build.getCpu().getSocket().trim();
            String moboSocket = build.getMotherboard().getSocket().trim();
            if (!cpuSocket.equalsIgnoreCase(moboSocket)) {
                issues.add(new CompatibilityIssue(
                        "Socket Mismatch",
                        String.format("CPU socket '%s' does not match Motherboard socket '%s'. The CPU cannot physically seat into this motherboard.",
                                cpuSocket, moboSocket),
                        CompatibilityStatus.INCOMPATIBLE
                ));
                worstStatus = CompatibilityStatus.INCOMPATIBLE;
            }
        }

        // Check if RAM type matches the motherboard memory standard
        if (build.getRam() != null && build.getMotherboard() != null) {
            String ramType = build.getRam().getRamType().trim();
            String moboRam = build.getMotherboard().getRamType().trim();
            if (!ramType.equalsIgnoreCase(moboRam)) {
                issues.add(new CompatibilityIssue(
                        "RAM Generation Mismatch",
                        String.format("RAM standard '%s' does not match Motherboard memory support '%s'. DDR generations are physically and electrically incompatible.",
                                ramType, moboRam),
                        CompatibilityStatus.INCOMPATIBLE
                ));
                worstStatus = CompatibilityStatus.INCOMPATIBLE;
            }
        }

        // Check if the power supply has enough wattage for CPU and GPU load
        if (build.getPsu() != null && (build.getCpu() != null || build.getGpu() != null)) {
            int psuWattage = build.getPsu().getWattage();
            int peakDraw = build.getEstimatedPeakPowerWatts();
            int recommendedWatts = build.getRecommendedPsuWatts();

            // Check if wattage falls below estimated peak system load
            if (psuWattage < peakDraw) {
                issues.add(new CompatibilityIssue(
                        "Insufficient PSU Wattage",
                        String.format("Selected PSU (%dW) is below the estimated peak load (%dW). System will experience brownouts or emergency shutdowns under heavy load.",
                                psuWattage, peakDraw),
                        CompatibilityStatus.INCOMPATIBLE
                ));
                worstStatus = CompatibilityStatus.INCOMPATIBLE;
            } else if (psuWattage < recommendedWatts) {
                // Warn the user if headroom is tighter than 20%
                issues.add(new CompatibilityIssue(
                        "Tight Power Headroom",
                        String.format("PSU capacity (%dW) covers estimated load (%dW) but leaves less than 20%% safety headroom (recommended: %dW). Transient spikes may trigger OCP.",
                                psuWattage, peakDraw, recommendedWatts),
                        CompatibilityStatus.WARNING
                ));
                if (worstStatus != CompatibilityStatus.INCOMPATIBLE) {
                    worstStatus = CompatibilityStatus.WARNING;
                }
            }

            // Check if power supply meets GPU manufacturer recommended wattage
            if (build.getGpu() != null && build.getGpu().getRecommendedPsuWatts() > 0) {
                int gpuRecommended = build.getGpu().getRecommendedPsuWatts();
                if (psuWattage < gpuRecommended && worstStatus != CompatibilityStatus.INCOMPATIBLE) {
                    issues.add(new CompatibilityIssue(
                            "Below GPU Recommended PSU",
                            String.format("GPU manufacturer recommends at least a %dW power supply (selected: %dW).",
                                    gpuRecommended, psuWattage),
                            CompatibilityStatus.WARNING
                    ));
                    if (worstStatus != CompatibilityStatus.INCOMPATIBLE) {
                        worstStatus = CompatibilityStatus.WARNING;
                    }
                }
            }
        }

        // Return the final result with worst status and all collected issues
        return new CompatibilityResult(worstStatus, issues);
    }
}
