package com.pcanalyzer.db;

import com.pcanalyzer.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for SqliteComponentDao using an isolated test SQLite database.
 */
class SqliteComponentDaoTest {

    @TempDir
    Path tempDir;

    private DatabaseManager dbManager;
    private SqliteComponentDao componentDao;

    @BeforeEach
    void setUp() {
        // Use an isolated temporary SQLite database file per test run
        String dbPath = tempDir.resolve("test_pcanalyzer.db").toAbsolutePath().toString();
        dbManager = new DatabaseManager("jdbc:sqlite:" + dbPath);
        dbManager.initialize();
        componentDao = new SqliteComponentDao(dbManager);
    }

    @Test
    @DisplayName("Initial schema creation and seed data loading")
    void testSeedDataPopulated() {
        List<Component> allComponents = componentDao.findAll();
        assertFalse(allComponents.isEmpty(), "Seed data should populate initial components");
        assertTrue(allComponents.size() >= 20, "Should have at least 20 seeded components (CPUs, GPUs, etc.)");

        List<Component> cpus = componentDao.findByType(ComponentType.CPU);
        assertTrue(cpus.size() >= 10, "Should have seeded at least 10 CPUs");

        List<Component> gpus = componentDao.findByType(ComponentType.GPU);
        assertTrue(gpus.size() >= 10, "Should have seeded at least 10 GPUs");
    }

    @Test
    @DisplayName("Polymorphic retrieval by ID")
    void testFindById() {
        List<Component> cpus = componentDao.findByType(ComponentType.CPU);
        assertFalse(cpus.isEmpty());

        Component firstCpu = cpus.get(0);
        Optional<Component> found = componentDao.findById(firstCpu.getId());

        assertTrue(found.isPresent());
        assertInstanceOf(Cpu.class, found.get());
        Cpu cpu = (Cpu) found.get();
        assertEquals(firstCpu.getName(), cpu.getName());
        assertNotNull(cpu.getSocket());
        assertTrue(cpu.getCores() > 0);
    }

    @Test
    @DisplayName("Insert, update, and delete a new CPU")
    void testCpuCrudLifecycle() {
        Cpu newCpu = new Cpu(null, "AMD", "Ryzen 9 7950X", 549.99, 170, "AM5", 16, 32, 4.5, 5.7, 63000);
        Component saved = componentDao.save(newCpu);

        assertNotNull(saved.getId(), "Generated ID should be assigned");
        assertEquals("Ryzen 9 7950X", saved.getName());

        // Update price
        saved.setPrice(499.99);
        Component updated = componentDao.save(saved);
        assertEquals(499.99, updated.getPrice(), 0.001);

        // Delete
        boolean deleted = componentDao.deleteById(saved.getId());
        assertTrue(deleted, "Delete should succeed");
        assertTrue(componentDao.findById(saved.getId()).isEmpty(), "Component should no longer exist");
    }

    @Test
    @DisplayName("Insert, update, and delete a new GPU")
    void testGpuCrudLifecycle() {
        Gpu newGpu = new Gpu(null, "NVIDIA", "GeForce RTX 4070", 549.99, 200, 12, 200, 650, 140.0, 98.0, 52.0);
        Component saved = componentDao.save(newGpu);

        assertNotNull(saved.getId());
        assertInstanceOf(Gpu.class, saved);
        Gpu savedGpu = (Gpu) saved;
        assertEquals(12, savedGpu.getVramGb());
        assertEquals(140.0, savedGpu.getFps1080p(), 0.01);

        // Delete
        assertTrue(componentDao.deleteById(saved.getId()));
        assertTrue(componentDao.findById(saved.getId()).isEmpty());
    }
}
