package com.pcanalyzer.model;

import java.util.Objects;

/**
 * Represents a Central Processing Unit (CPU).
 *
 * Design Decision:
 * The CPU model captures hardware attributes necessary for:
 * 1. Physical compatibility checking (motherboard socket match).
 * 2. Benchmark ranking (multithreaded synthetic benchmark score).
 * 3. Power budget calculation (TDP inherited from Component).
 */
public class Cpu extends Component {

    private String socket;
    private int cores;
    private int threads;
    private double baseClockGhz;
    private double boostClockGhz;
    private int benchmarkScore;

    public Cpu(Integer id, String brand, String name, double price, int tdpWatts,
               String socket, int cores, int threads, double baseClockGhz, double boostClockGhz, int benchmarkScore) {
        super(id, ComponentType.CPU, brand, name, price, tdpWatts);
        this.socket = Objects.requireNonNull(socket, "Socket must not be null");
        this.cores = Math.max(1, cores);
        this.threads = Math.max(1, threads);
        this.baseClockGhz = Math.max(0.0, baseClockGhz);
        this.boostClockGhz = Math.max(0.0, boostClockGhz);
        this.benchmarkScore = Math.max(0, benchmarkScore);
    }

    public String getSocket() {
        return socket;
    }

    public void setSocket(String socket) {
        this.socket = socket;
    }

    public int getCores() {
        return cores;
    }

    public void setCores(int cores) {
        this.cores = Math.max(1, cores);
    }

    public int getThreads() {
        return threads;
    }

    public void setThreads(int threads) {
        this.threads = Math.max(1, threads);
    }

    public double getBaseClockGhz() {
        return baseClockGhz;
    }

    public void setBaseClockGhz(double baseClockGhz) {
        this.baseClockGhz = Math.max(0.0, baseClockGhz);
    }

    public double getBoostClockGhz() {
        return boostClockGhz;
    }

    public void setBoostClockGhz(double boostClockGhz) {
        this.boostClockGhz = Math.max(0.0, boostClockGhz);
    }

    public int getBenchmarkScore() {
        return benchmarkScore;
    }

    public void setBenchmarkScore(int benchmarkScore) {
        this.benchmarkScore = Math.max(0, benchmarkScore);
    }

    @Override
    public String getKeySpecs() {
        return String.format("%s | %dC/%dT | %.1f-%.1f GHz | Score: %,d",
                socket, cores, threads, baseClockGhz, boostClockGhz, benchmarkScore);
    }
}
