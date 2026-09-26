package com.pcanalyzer.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

// Saves and reads price history using SQLite database tables
public class SqlitePriceHistoryDao implements PriceHistoryDao {

    private final DatabaseManager dbManager;

    // Attach our database connection manager
    public SqlitePriceHistoryDao(DatabaseManager dbManager) {
        this.dbManager = Objects.requireNonNull(dbManager, "DatabaseManager cannot be null");
    }

    // Insert a new price log entry into SQLite
    @Override
    public void recordPrice(int componentId, double price, String source) {
        String sql = "INSERT INTO price_history (component_id, price, source) VALUES (?, ?, ?)";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, componentId);
            pstmt.setDouble(2, price);
            pstmt.setString(3, Objects.requireNonNullElse(source, "UNKNOWN"));
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[SqlitePriceHistoryDao] recordPrice error: " + e.getMessage());
        }
    }

    // Read all recorded prices for a specific part ordered by date
    @Override
    public List<PriceRecord> getHistoryForComponent(int componentId) {
        String sql = "SELECT id, component_id, price, source, fetched_at FROM price_history WHERE component_id = ? ORDER BY fetched_at ASC";
        List<PriceRecord> list = new ArrayList<>();
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, componentId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(new PriceRecord(
                            rs.getInt("id"),
                            rs.getInt("component_id"),
                            rs.getDouble("price"),
                            rs.getString("source"),
                            rs.getString("fetched_at")
                    ));
                }
            }
        } catch (SQLException e) {
            System.err.println("[SqlitePriceHistoryDao] getHistoryForComponent error: " + e.getMessage());
        }
        return list;
    }
}
