package com.pcanalyzer.db;

import java.util.List;

// Rules for tracking and reading historical price changes for parts
public interface PriceHistoryDao {

    // A single historical price check record
    record PriceRecord(int id, int componentId, double price, String source, String fetchedAt) {}

    // Save a new price checkpoint for a part
    void recordPrice(int componentId, double price, String source);

    // Get the full price history for one specific part
    List<PriceRecord> getHistoryForComponent(int componentId);
}
