package com.pcanalyzer.db;

import java.util.List;

/**
 * Data Access Object for historical component price snapshots.
 */
public interface PriceHistoryDao {

    /**
     * Value record representing a historical price snapshot.
     */
    record PriceRecord(int id, int componentId, double price, String source, String fetchedAt) {}

    /**
     * Records a new price snapshot for a component.
     *
     * @param componentId The component ID
     * @param price The recorded price
     * @param source Data source (e.g. "SEED", "MANUAL", "API")
     */
    void recordPrice(int componentId, double price, String source);

    /**
     * Retrieves all price history records for a given component in chronological order.
     *
     * @param componentId The component ID
     * @return List of price records
     */
    List<PriceRecord> getHistoryForComponent(int componentId);
}
