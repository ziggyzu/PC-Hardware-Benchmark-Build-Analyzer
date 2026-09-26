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
}
