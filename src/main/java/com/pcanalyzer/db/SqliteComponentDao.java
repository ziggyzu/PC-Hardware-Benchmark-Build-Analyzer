package com.pcanalyzer.db;

import com.pcanalyzer.model.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

// Handles loading, inserting, updating, and deleting parts from SQLite tables
public class SqliteComponentDao implements ComponentDao {

    private final DatabaseManager dbManager;

    // Single query joining the base parts table with all specification tables
    private static final String BASE_SELECT_QUERY = """
        SELECT c.id, c.type, c.brand, c.name, c.price, c.tdp_watts,
               cs.socket AS cpu_socket, cs.cores, cs.threads, cs.base_clock, cs.boost_clock, cs.benchmark_score,
               gs.vram_gb, gs.board_power_watts, gs.recommended_psu_watts, gs.fps_1080p, gs.fps_1440p, gs.fps_4k,
               ms.socket AS mobo_socket, ms.ram_type AS mobo_ram_type,
               rs.ram_type AS ram_type, rs.capacity_gb, rs.speed_mhz,
               ps.wattage, ps.efficiency_rating
        FROM components c
        LEFT JOIN cpu_specs cs ON c.id = cs.component_id
        LEFT JOIN gpu_specs gs ON c.id = gs.component_id
        LEFT JOIN motherboard_specs ms ON c.id = ms.component_id
        LEFT JOIN ram_specs rs ON c.id = rs.component_id
        LEFT JOIN psu_specs ps ON c.id = ps.component_id
    """;

    // Set up with our database manager
    public SqliteComponentDao(DatabaseManager dbManager) {
        this.dbManager = Objects.requireNonNull(dbManager, "DatabaseManager cannot be null");
    }

    // Read every component in the catalog from SQLite
    @Override
    public List<Component> findAll() {
        String sql = BASE_SELECT_QUERY + " ORDER BY c.type, c.brand, c.name";
        List<Component> list = new ArrayList<>();
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                Component comp = mapRowToComponent(rs);
                if (comp != null) list.add(comp);
            }
        } catch (SQLException e) {
            System.err.println("[SqliteComponentDao] findAll error: " + e.getMessage());
        }
        return list;
    }

    // Read only components of a chosen type like CPU or GPU
    @Override
    public List<Component> findByType(ComponentType type) {
        if (type == null) return findAll();
        String sql = BASE_SELECT_QUERY + " WHERE c.type = ? ORDER BY c.brand, c.name";
        List<Component> list = new ArrayList<>();
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, type.name());
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Component comp = mapRowToComponent(rs);
                    if (comp != null) list.add(comp);
                }
            }
        } catch (SQLException e) {
            System.err.println("[SqliteComponentDao] findByType error: " + e.getMessage());
        }
        return list;
    }

    // Look up one specific component by its database ID
    @Override
    public Optional<Component> findById(int id) {
        String sql = BASE_SELECT_QUERY + " WHERE c.id = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.ofNullable(mapRowToComponent(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[SqliteComponentDao] findById error: " + e.getMessage());
        }
        return Optional.empty();
    }

    // Save a component (inserts if new, updates if already existing)
    @Override
    public Component save(Component component) {
        Objects.requireNonNull(component, "Component cannot be null");
        if (component.getId() == null) {
            return insertComponent(component);
        } else {
            return updateComponent(component);
        }
    }

    // Delete a component by its ID number
    @Override
    public boolean deleteById(int id) {
        String sql = "DELETE FROM components WHERE id = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            int rows = pstmt.executeUpdate();
            return rows > 0;
        } catch (SQLException e) {
            System.err.println("[SqliteComponentDao] deleteById error: " + e.getMessage());
            return false;
        }
    }

    // Insert a new component and its specs into the database
    private Component insertComponent(Component comp) {
        String baseSql = "INSERT INTO components (type, brand, name, price, tdp_watts) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = dbManager.getConnection()) {
            conn.setAutoCommit(false);
            try {
                int generatedId;
                try (PreparedStatement pstmt = conn.prepareStatement(baseSql, Statement.RETURN_GENERATED_KEYS)) {
                    pstmt.setString(1, comp.getType().name());
                    pstmt.setString(2, comp.getBrand());
                    pstmt.setString(3, comp.getName());
                    pstmt.setDouble(4, comp.getPrice());
                    pstmt.setInt(5, comp.getTdpWatts());
                    pstmt.executeUpdate();

                    try (ResultSet rs = pstmt.getGeneratedKeys()) {
                        if (rs.next()) {
                            generatedId = rs.getInt(1);
                            comp.setId(generatedId);
                        } else {
                            throw new SQLException("Failed to retrieve generated ID for component.");
                        }
                    }
                }

                // Insert the matching hardware specs row
                insertSpecs(conn, comp);

                // Add an entry in the price history table
                insertPriceHistory(conn, generatedId, comp.getPrice(), "MANUAL_ENTRY");

                conn.commit();
                return comp;
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            System.err.println("[SqliteComponentDao] insert error: " + e.getMessage());
            throw new RuntimeException("Failed to insert component: " + comp.getName(), e);
        }
    }

    // Update an existing component and its specs in the database
    private Component updateComponent(Component comp) {
        String baseSql = "UPDATE components SET brand = ?, name = ?, price = ?, tdp_watts = ? WHERE id = ?";
        try (Connection conn = dbManager.getConnection()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement pstmt = conn.prepareStatement(baseSql)) {
                    pstmt.setString(1, comp.getBrand());
                    pstmt.setString(2, comp.getName());
                    pstmt.setDouble(3, comp.getPrice());
                    pstmt.setInt(4, comp.getTdpWatts());
                    pstmt.setInt(5, comp.getId());
                    pstmt.executeUpdate();
                }

                // Update the matching hardware specs row
                updateSpecs(conn, comp);

                // Record the updated price in the history table
                insertPriceHistory(conn, comp.getId(), comp.getPrice(), "PRICE_UPDATE");

                conn.commit();
                return comp;
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            System.err.println("[SqliteComponentDao] update error: " + e.getMessage());
            throw new RuntimeException("Failed to update component: " + comp.getName(), e);
        }
    }

    // Insert category-specific details into the right specification table
    private void insertSpecs(Connection conn, Component comp) throws SQLException {
        switch (comp.getType()) {
            case CPU -> {
                Cpu cpu = (Cpu) comp;
                String sql = "INSERT INTO cpu_specs (component_id, socket, cores, threads, base_clock, boost_clock, benchmark_score) VALUES (?, ?, ?, ?, ?, ?, ?)";
                try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setInt(1, cpu.getId());
                    pstmt.setString(2, cpu.getSocket());
                    pstmt.setInt(3, cpu.getCores());
                    pstmt.setInt(4, cpu.getThreads());
                    pstmt.setDouble(5, cpu.getBaseClockGhz());
                    pstmt.setDouble(6, cpu.getBoostClockGhz());
                    pstmt.setInt(7, cpu.getBenchmarkScore());
                    pstmt.executeUpdate();
                }
            }
            case GPU -> {
                Gpu gpu = (Gpu) comp;
                String sql = "INSERT INTO gpu_specs (component_id, vram_gb, board_power_watts, recommended_psu_watts, fps_1080p, fps_1440p, fps_4k) VALUES (?, ?, ?, ?, ?, ?, ?)";
                try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setInt(1, gpu.getId());
                    pstmt.setInt(2, gpu.getVramGb());
                    pstmt.setInt(3, gpu.getBoardPowerWatts());
                    pstmt.setInt(4, gpu.getRecommendedPsuWatts());
                    pstmt.setDouble(5, gpu.getFps1080p());
                    pstmt.setDouble(6, gpu.getFps1440p());
                    pstmt.setDouble(7, gpu.getFps4k());
                    pstmt.executeUpdate();
                }
            }
            case MOTHERBOARD -> {
                Motherboard mobo = (Motherboard) comp;
                String sql = "INSERT INTO motherboard_specs (component_id, socket, ram_type) VALUES (?, ?, ?)";
                try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setInt(1, mobo.getId());
                    pstmt.setString(2, mobo.getSocket());
                    pstmt.setString(3, mobo.getRamType());
                    pstmt.executeUpdate();
                }
            }
            case RAM -> {
                Ram ram = (Ram) comp;
                String sql = "INSERT INTO ram_specs (component_id, ram_type, capacity_gb, speed_mhz) VALUES (?, ?, ?, ?)";
                try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setInt(1, ram.getId());
                    pstmt.setString(2, ram.getRamType());
                    pstmt.setInt(3, ram.getCapacityGb());
                    pstmt.setInt(4, ram.getSpeedMhz());
                    pstmt.executeUpdate();
                }
            }
            case PSU -> {
                Psu psu = (Psu) comp;
                String sql = "INSERT INTO psu_specs (component_id, wattage, efficiency_rating) VALUES (?, ?, ?)";
                try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setInt(1, psu.getId());
                    pstmt.setInt(2, psu.getWattage());
                    pstmt.setString(3, psu.getEfficiencyRating());
                    pstmt.executeUpdate();
                }
            }
        }
    }

    // Update category-specific details in the right specification table
    private void updateSpecs(Connection conn, Component comp) throws SQLException {
        switch (comp.getType()) {
            case CPU -> {
                Cpu cpu = (Cpu) comp;
                String sql = "UPDATE cpu_specs SET socket = ?, cores = ?, threads = ?, base_clock = ?, boost_clock = ?, benchmark_score = ? WHERE component_id = ?";
                try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setString(1, cpu.getSocket());
                    pstmt.setInt(2, cpu.getCores());
                    pstmt.setInt(3, cpu.getThreads());
                    pstmt.setDouble(4, cpu.getBaseClockGhz());
                    pstmt.setDouble(5, cpu.getBoostClockGhz());
                    pstmt.setInt(6, cpu.getBenchmarkScore());
                    pstmt.setInt(7, cpu.getId());
                    pstmt.executeUpdate();
                }
            }
            case GPU -> {
                Gpu gpu = (Gpu) comp;
                String sql = "UPDATE gpu_specs SET vram_gb = ?, board_power_watts = ?, recommended_psu_watts = ?, fps_1080p = ?, fps_1440p = ?, fps_4k = ? WHERE component_id = ?";
                try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setInt(1, gpu.getVramGb());
                    pstmt.setInt(2, gpu.getBoardPowerWatts());
                    pstmt.setInt(3, gpu.getRecommendedPsuWatts());
                    pstmt.setDouble(4, gpu.getFps1080p());
                    pstmt.setDouble(5, gpu.getFps1440p());
                    pstmt.setDouble(6, gpu.getFps4k());
                    pstmt.setInt(7, gpu.getId());
                    pstmt.executeUpdate();
                }
            }
            case MOTHERBOARD -> {
                Motherboard mobo = (Motherboard) comp;
                String sql = "UPDATE motherboard_specs SET socket = ?, ram_type = ? WHERE component_id = ?";
                try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setString(1, mobo.getSocket());
                    pstmt.setString(2, mobo.getRamType());
                    pstmt.setInt(3, mobo.getId());
                    pstmt.executeUpdate();
                }
            }
            case RAM -> {
                Ram ram = (Ram) comp;
                String sql = "UPDATE ram_specs SET ram_type = ?, capacity_gb = ?, speed_mhz = ? WHERE component_id = ?";
                try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setString(1, ram.getRamType());
                    pstmt.setInt(2, ram.getCapacityGb());
                    pstmt.setInt(3, ram.getSpeedMhz());
                    pstmt.setInt(4, ram.getId());
                    pstmt.executeUpdate();
                }
            }
            case PSU -> {
                Psu psu = (Psu) comp;
                String sql = "UPDATE psu_specs SET wattage = ?, efficiency_rating = ? WHERE component_id = ?";
                try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setInt(1, psu.getWattage());
                    pstmt.setString(2, psu.getEfficiencyRating());
                    pstmt.setInt(3, psu.getId());
                    pstmt.executeUpdate();
                }
            }
        }
    }

    // Add a price check entry into the price history table
    private void insertPriceHistory(Connection conn, int componentId, double price, String source) throws SQLException {
        String sql = "INSERT INTO price_history (component_id, price, source) VALUES (?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, componentId);
            pstmt.setDouble(2, price);
            pstmt.setString(3, source);
            pstmt.executeUpdate();
        }
    }

    // Convert an SQL database row into the proper Java component object
    private Component mapRowToComponent(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        String typeStr = rs.getString("type");
        ComponentType type = ComponentType.fromString(typeStr);
        String brand = rs.getString("brand");
        String name = rs.getString("name");
        double price = rs.getDouble("price");
        int tdpWatts = rs.getInt("tdp_watts");

        if (type == null) return null;

        return switch (type) {
            case CPU -> new Cpu(
                    id, brand, name, price, tdpWatts,
                    rs.getString("cpu_socket"),
                    rs.getInt("cores"),
                    rs.getInt("threads"),
                    rs.getDouble("base_clock"),
                    rs.getDouble("boost_clock"),
                    rs.getInt("benchmark_score")
            );
            case GPU -> new Gpu(
                    id, brand, name, price, tdpWatts,
                    rs.getInt("vram_gb"),
                    rs.getInt("board_power_watts"),
                    rs.getInt("recommended_psu_watts"),
                    rs.getDouble("fps_1080p"),
                    rs.getDouble("fps_1440p"),
                    rs.getDouble("fps_4k")
            );
            case MOTHERBOARD -> new Motherboard(
                    id, brand, name, price, tdpWatts,
                    rs.getString("mobo_socket"),
                    rs.getString("mobo_ram_type")
            );
            case RAM -> new Ram(
                    id, brand, name, price, tdpWatts,
                    rs.getString("ram_type"),
                    rs.getInt("capacity_gb"),
                    rs.getInt("speed_mhz")
            );
            case PSU -> new Psu(
                    id, brand, name, price, tdpWatts,
                    rs.getInt("wattage"),
                    rs.getString("efficiency_rating")
            );
        };
    }
}
