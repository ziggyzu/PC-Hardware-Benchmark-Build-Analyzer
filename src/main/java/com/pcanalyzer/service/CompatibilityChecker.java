package com.pcanalyzer.service;

import com.pcanalyzer.model.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Service that evaluates hardware compatibility rules for a PC build.
 *
 * Design Decision:
 * 1. Decoupled Business Logic: Compatibility checks are pure algorithmic calculations,
 *    isolated from UI views and databases. This makes it trivial to unit-test and extend
 *    with new rules without breaking controllers.
 * 2. Structured Diagnostics: Rather than aborting at the first failure or returning
 *    a simple boolean, this service evaluates all rules and collects detailed diagnostics
 *    with severity ratings (INCOMPATIBLE, WARNING).
 */
public class CompatibilityChecker {

    /**
     * Evaluates all compatibility rules for the given build configuration.
     *
     * @param build Build containing selected components
     * @return CompatibilityResult containing status and itemized diagnostic messages
     */
    public CompatibilityResult checkCompatibility(Build build) {
        if (build == null) {
            return CompatibilityResult.compatible();
        }

        List<CompatibilityIssue> issues = new ArrayList<>();
        CompatibilityStatus worstStatus = CompatibilityStatus.COMPATIBLE;

        // Rule 1: CPU Socket vs Motherboard Socket
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

        // Rule 2: RAM Generation vs Motherboard RAM Support
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

        // Rule 3: Power Supply Capacity and Headroom Margin (~20%)
        if (build.getPsu() != null && (build.getCpu() != null || build.getGpu() != null)) {
            int psuWattage = build.getPsu().getWattage();
            int peakDraw = build.getEstimatedPeakPowerWatts();
            int recommendedWatts = build.getRecommendedPsuWatts();

            if (psuWattage < peakDraw) {
                issues.add(new CompatibilityIssue(
                        "Insufficient PSU Wattage",
                        String.format("Selected PSU (%dW) is below the estimated peak load (%dW). System will experience brownouts or emergency shutdowns under heavy load.",
                                psuWattage, peakDraw),
                        CompatibilityStatus.INCOMPATIBLE
                ));
                worstStatus = CompatibilityStatus.INCOMPATIBLE;
            } else if (psuWattage < recommendedWatts) {
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

            // Rule 3b: GPU specific vendor recommendation
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

        return new CompatibilityResult(worstStatus, issues);
    }
}
