package com.pcanalyzer.service;

import com.pcanalyzer.db.DatabaseManager;
import com.pcanalyzer.db.PriceHistoryDao;
import com.pcanalyzer.db.SqliteComponentDao;
import com.pcanalyzer.db.SqlitePriceHistoryDao;
import com.pcanalyzer.model.Component;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

public class HardwareSyncServiceTest {

    private static final String TEST_DB = "test_sync.db";
    private DatabaseManager dbManager;
    private SqliteComponentDao componentDao;
    private PriceHistoryDao priceHistoryDao;

    @BeforeEach
    public void setUp() {
        deleteTestDb();
        dbManager = new DatabaseManager("jdbc:sqlite:" + TEST_DB);
        dbManager.initialize();
        componentDao = new SqliteComponentDao(dbManager);
        priceHistoryDao = new SqlitePriceHistoryDao(dbManager);
    }

    @AfterEach
    public void tearDown() {
        deleteTestDb();
    }

    private void deleteTestDb() {
        File f = new File(TEST_DB);
        if (f.exists()) f.delete();
    }

    @Test
    public void testSyncServiceExecutesAsynchronouslyAndParsesJson() throws Exception {
        HardwareSyncService syncService = new HardwareSyncService(componentDao, priceHistoryDao);

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<HardwareSyncService.SyncResult> resultRef = new AtomicReference<>();
        AtomicReference<Throwable> errorRef = new AtomicReference<>();

        syncService.syncAsynchronously(
                res -> {
                    resultRef.set(res);
                    latch.countDown();
                },
                err -> {
                    errorRef.set(err);
                    latch.countDown();
                }
        );

        boolean completed = latch.await(10, TimeUnit.SECONDS);
        assertTrue(completed, "Sync task should complete within 10 seconds");
        assertNull(errorRef.get(), "Sync should not throw an unhandled error");
        assertNotNull(resultRef.get(), "SyncResult should be populated");
        assertTrue(resultRef.get().success(), "SyncResult should report success");

        // Verify that database reflects valid parsed data
        List<Component> all = componentDao.findAll();
        assertFalse(all.isEmpty(), "Catalog should contain hardware components");
    }

    @Test
    public void testSyncFromLocalJsonFile() throws Exception {
        HardwareSyncService syncService = new HardwareSyncService(componentDao, priceHistoryDao);
        File localFile = new File("data/hardware-pricing.json");

        assertTrue(localFile.exists(), "Local JSON data file should exist at data/hardware-pricing.json");

        HardwareSyncService.SyncResult result = syncService.performSyncFromLocalFile(localFile);
        assertNotNull(result, "SyncResult should not be null");
        assertTrue(result.success(), "Local JSON file sync should succeed");
        assertNotNull(result.rawJson(), "Raw JSON payload should be populated");
        assertTrue(result.rawJson().contains("Ryzen 5 5600"), "Raw JSON should contain parsed item Ryzen 5 5600");
    }

    @Test
    public void testParseAndApplyJsonString() throws Exception {
        HardwareSyncService syncService = new HardwareSyncService(componentDao, priceHistoryDao);

        String customJson = """
            [
                {"brand": "AMD", "name": "Ryzen 5 5600", "price": 99.99, "source": "UnitTest_Source"}
            ]
            """;

        HardwareSyncService.SyncResult result = syncService.parseAndApplyJson(
                customJson, "UnitTest", "Unit Test Execution", Thread.currentThread().getName()
        );

        assertNotNull(result);
        assertTrue(result.success());
        assertEquals(1, result.updatedCount(), "Should update exactly 1 matching component price");

        Component comp = componentDao.findAll().stream()
                .filter(c -> c.getName().equals("Ryzen 5 5600"))
                .findFirst()
                .orElse(null);

        assertNotNull(comp, "Component Ryzen 5 5600 should exist in database");
        assertEquals(99.99, comp.getPrice(), 0.001, "Component price should be updated to 99.99");
    }
}

