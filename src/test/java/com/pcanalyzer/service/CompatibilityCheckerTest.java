package com.pcanalyzer.service;

import com.pcanalyzer.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for CompatibilityChecker business rules.
 */
class CompatibilityCheckerTest {

    private CompatibilityChecker checker;

    @BeforeEach
    void setUp() {
        checker = new CompatibilityChecker();
    }

    @Test
    @DisplayName("Compatible build passes all checks")
    void testCompatibleBuild() {
        Build build = new Build();
        build.setCpu(new Cpu(1, "AMD", "Ryzen 5 7600", 189.99, 65, "AM5", 6, 12, 3.8, 5.1, 27500));
        build.setMotherboard(new Motherboard(2, "ASUS", "TUF B650", 199.99, 30, "AM5", "DDR5"));
        build.setRam(new Ram(3, "Corsair", "Vengeance", 109.99, 15, "DDR5", 32, 6000));
        build.setGpu(new Gpu(4, "NVIDIA", "GeForce RTX 4060", 299.99, 115, 8, 115, 500, 96.5, 66.0, 34.0));
        build.setPsu(new Psu(5, "Seasonic", "FOCUS 650W", 99.99, 0, 650, "80+ Gold"));

        CompatibilityResult result = checker.checkCompatibility(build);
        assertTrue(result.isCompatible(), "Valid configuration should be COMPATIBLE");
        assertEquals(CompatibilityStatus.COMPATIBLE, result.getStatus());
        assertTrue(result.getIssues().isEmpty());
    }

    @Test
    @DisplayName("Socket mismatch generates INCOMPATIBLE error")
    void testSocketMismatch() {
        Build build = new Build();
        // AM4 CPU with AM5 Motherboard
        build.setCpu(new Cpu(1, "AMD", "Ryzen 5 5600", 124.99, 65, "AM4", 6, 12, 3.5, 4.4, 21500));
        build.setMotherboard(new Motherboard(2, "ASUS", "TUF B650", 199.99, 30, "AM5", "DDR5"));

        CompatibilityResult result = checker.checkCompatibility(build);
        assertTrue(result.isIncompatible(), "Socket mismatch must be INCOMPATIBLE");
        assertTrue(result.getIssues().stream().anyMatch(i -> i.getRuleName().contains("Socket")));
    }

    @Test
    @DisplayName("RAM standard mismatch generates INCOMPATIBLE error")
    void testRamMismatch() {
        Build build = new Build();
        build.setMotherboard(new Motherboard(1, "MSI", "B760 DDR4", 139.99, 25, "LGA1700", "DDR4"));
        build.setRam(new Ram(2, "Kingston", "Fury DDR5", 59.99, 10, "DDR5", 16, 5600));

        CompatibilityResult result = checker.checkCompatibility(build);
        assertTrue(result.isIncompatible(), "RAM standard mismatch must be INCOMPATIBLE");
        assertTrue(result.getIssues().stream().anyMatch(i -> i.getRuleName().contains("RAM")));
    }

    @Test
    @DisplayName("Insufficient PSU capacity generates INCOMPATIBLE error")
    void testInsufficientPsu() {
        Build build = new Build();
        // CPU TDP 125W + GPU 245W + overhead 70W = 440W
        build.setCpu(new Cpu(1, "Intel", "Core i5-13600K", 269.99, 125, "LGA1700", 14, 20, 3.5, 5.1, 38200));
        build.setGpu(new Gpu(2, "AMD", "Radeon RX 7700 XT", 419.99, 245, 12, 245, 700, 135.0, 96.5, 54.0));
        // PSU is only 400W (< 440W)
        build.setPsu(new Psu(3, "EVGA", "400W", 39.99, 0, 400, "80+ White"));

        CompatibilityResult result = checker.checkCompatibility(build);
        assertTrue(result.isIncompatible(), "PSU below estimated peak draw must be INCOMPATIBLE");
    }

    @Test
    @DisplayName("Tight PSU headroom generates WARNING")
    void testTightPsuHeadroomWarning() {
        Build build = new Build();
        // CPU 65W + GPU 115W + overhead 70W = 250W. Recommended = 250 * 1.20 = 300W
        build.setCpu(new Cpu(1, "AMD", "Ryzen 5 7600", 189.99, 65, "AM5", 6, 12, 3.8, 5.1, 27500));
        build.setGpu(new Gpu(2, "NVIDIA", "GeForce RTX 4060", 299.99, 115, 8, 115, 500, 96.5, 66.0, 34.0));
        // PSU is 275W (covers 250W, but < 300W and < GPU recommended 500W)
        build.setPsu(new Psu(3, "Custom", "275W PSU", 29.99, 0, 275, "Standard"));

        CompatibilityResult result = checker.checkCompatibility(build);
        assertTrue(result.hasWarnings(), "Tight headroom should yield WARNING");
        assertFalse(result.isIncompatible(), "Should not be incompatible if peak wattage is met");
    }
}
