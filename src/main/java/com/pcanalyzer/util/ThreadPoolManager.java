package com.pcanalyzer.util;

import javafx.concurrent.Task;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

// Manages our background worker threads so the app stays fast and responsive
public class ThreadPoolManager {

    // Keep 4 worker threads ready for background jobs
    private static final int DEFAULT_POOL_SIZE = 4;
    private static volatile ThreadPoolManager instance;

    private final ExecutorService executor;
    private final AtomicInteger threadCounter = new AtomicInteger(1);

    // Set up the thread pool with friendly names and daemon threads
    private ThreadPoolManager() {
        this(DEFAULT_POOL_SIZE);
    }

    private ThreadPoolManager(int poolSize) {
        ThreadFactory threadFactory = runnable -> {
            Thread thread = new Thread(runnable, "pcanalyzer-worker-" + threadCounter.getAndIncrement());
            thread.setDaemon(true);
            thread.setPriority(Thread.NORM_PRIORITY);
            return thread;
        };

        this.executor = Executors.newFixedThreadPool(poolSize, threadFactory);
    }

    // Get the single shared thread pool
    public static ThreadPoolManager getInstance() {
        if (instance == null) {
            synchronized (ThreadPoolManager.class) {
                if (instance == null) {
                    instance = new ThreadPoolManager();
                }
            }
        }
        return instance;
    }

    // Run a quick background task
    public void execute(Runnable task) {
        executor.execute(task);
    }

    // Run a background task that returns a result
    public <T> Future<T> submit(Callable<T> task) {
        return executor.submit(task);
    }

    // Run a JavaFX background task
    public <T> void submitTask(Task<T> task) {
        executor.submit(task);
    }

    // Get the underlying thread pool service
    public ExecutorService getExecutor() {
        return executor;
    }

    // Stop all worker threads when the app closes
    public void shutdown() {
        try {
            executor.shutdown();
            if (!executor.awaitTermination(2, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
