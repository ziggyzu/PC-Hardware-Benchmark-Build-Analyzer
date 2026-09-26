package com.pcanalyzer.util;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

public class ThreadPoolManagerTest {

    @Test
    public void testThreadPoolExecutesOnNamedWorkerThread() throws Exception {
        ThreadPoolManager pool = ThreadPoolManager.getInstance();
        assertNotNull(pool.getExecutor());

        AtomicReference<String> executingThreadName = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        pool.execute(() -> {
            executingThreadName.set(Thread.currentThread().getName());
            latch.countDown();
        });

        boolean completed = latch.await(3, TimeUnit.SECONDS);
        assertTrue(completed, "Task should complete within 3 seconds");
        assertNotNull(executingThreadName.get());
        assertTrue(executingThreadName.get().startsWith("pcanalyzer-worker-"), 
                "Thread should have custom prefix 'pcanalyzer-worker-', but got: " + executingThreadName.get());
    }

    @Test
    public void testThreadPoolSubmitCallableReturnsValue() throws Exception {
        ThreadPoolManager pool = ThreadPoolManager.getInstance();
        Future<Integer> future = pool.submit(() -> 42 * 2);

        Integer result = future.get(3, TimeUnit.SECONDS);
        assertEquals(84, result);
    }

    @Test
    public void testWorkerThreadsAreDaemon() throws Exception {
        ThreadPoolManager pool = ThreadPoolManager.getInstance();
        AtomicBoolean isDaemon = new AtomicBoolean(false);
        CountDownLatch latch = new CountDownLatch(1);

        pool.execute(() -> {
            isDaemon.set(Thread.currentThread().isDaemon());
            latch.countDown();
        });

        latch.await(3, TimeUnit.SECONDS);
        assertTrue(isDaemon.get(), "Worker threads must be daemons to avoid blocking app exit");
    }
}
