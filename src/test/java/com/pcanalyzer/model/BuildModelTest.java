package com.pcanalyzer.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Build model domain calculations.
 */
class BuildModelTest {

    @Test
    @DisplayName("Calculate total price across selected components")
    void testTotalPriceCalculation() {
        Build build = new Build("Test Rig");
        Cpu cpu = new Cpu(1, "AMD", "Ryzen 5 7600", 189.99, 65, "AM5", 6, 12, 3.8, 5.1, 27500);
        Gpu gpu = new Gpu(2, "NVIDIA", "GeForce RTX 4060", 299.99, 115, 8, 115, 500, 96.5, 66.0, 34.0);
        Motherboard mobo = new Motherboard(3, "ASUS", "TUF B650", 199.99, 30, "AM5", "DDR5");
        Ram ram = new Ram(4, "Corsair", "Vengeance DDR5", 109.99, 15, "DDR5", 32, 6000);
        Psu psu = new Psu(5, "Corsair", "RM750e", 109.99, 0, 750, "80+ Gold");

        build.setCpu(cpu);
        build.setGpu(gpu);
        build.setMotherboard(mobo);
        build.setRam(ram);
        build.setPsu(psu);

        double expectedPrice = 189.99 + 299.99 + 199.99 + 109.99 + 109.99;
        assertEquals(expectedPrice, build.getTotalPrice(), 0.001);
    }

    @Test
    @DisplayName("Calculate total TDP and estimated peak system wattage")
    void testPowerCalculations() {
        Build build = new Build();
        Cpu cpu = new Cpu(1, "Intel", "Core i5-13600K", 269.99, 125, "LGA1700", 14, 20, 3.5, 5.1, 38200);
        Gpu gpu = new Gpu(2, "AMD", "Radeon RX 6700 XT", 329.99, 230, 12, 230, 650, 112.4, 79.1, 44.2);

        build.setCpu(cpu);
        build.setGpu(gpu);

        // TDP = 125 + 230 = 355
        assertEquals(355, build.getTotalTdpWatts());

        // Estimated Peak = 355 + 70 (overhead) = 425
        assertEquals(425, build.getEstimatedPeakPowerWatts());

        // Recommended PSU = 425 * 1.20 = 510
        assertEquals(510, build.getRecommendedPsuWatts());
    }

    @Test
    @DisplayName("Polymorphic component slot assignment and removal")
    void testSlotAssignment() {
        Build build = new Build();
        Cpu cpu = new Cpu(1, "AMD", "Ryzen 5 5600", 124.99, 65, "AM4", 6, 12, 3.5, 4.4, 21500);

        build.setComponent(cpu);
        assertSame(cpu, build.getCpu());

        build.removeComponent(cpu);
        assertNull(build.getCpu());
    }
}
