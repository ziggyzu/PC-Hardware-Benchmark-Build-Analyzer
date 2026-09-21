package com.pcanalyzer.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Manages SQLite database connections, idempotent schema creation, and seeding.
 *
 * Design Decision:
 * 1. Plain JDBC with PreparedStatements: No heavy ORM (like Hibernate) which would add
 *    unnecessary overhead, complex configuration, and slow startup time to a desktop app.
 * 2. Configurable Connection URL: Allows runtime configuration of file-based storage
 *    (e.g., `jdbc:sqlite:pcanalyzer.db`) or in-memory storage (`jdbc:sqlite::memory:`)
 *    for lightning-fast isolated unit tests.
 * 3. Idempotent Migrations: `CREATE TABLE IF NOT EXISTS` ensures the application starts
 *    smoothly whether on a fresh install or existing database.
 * 4. Foreign Keys: SQLite requires explicit `PRAGMA foreign_keys = ON;` per connection.
 */
public class DatabaseManager {

    private static final String DEFAULT_DB_URL = "jdbc:sqlite:pcanalyzer.db";
    private final String dbUrl;

    public DatabaseManager() {
        this(DEFAULT_DB_URL);
    }

    public DatabaseManager(String dbUrl) {
        this.dbUrl = (dbUrl != null && !dbUrl.isBlank()) ? dbUrl : DEFAULT_DB_URL;
    }

    public String getDbUrl() {
        return dbUrl;
    }

    /**
     * Obtains a new database connection with foreign key enforcement enabled.
     *
     * @return Open Connection to SQLite database
     * @throws SQLException If connection fails
     */
    public Connection getConnection() throws SQLException {
        Connection conn = DriverManager.getConnection(dbUrl);
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA foreign_keys = ON;");
        }
        return conn;
    }

    /**
     * Initializes database schema idempotently and seeds starter components if empty.
     */
    public void initialize() {
        try (Connection conn = getConnection()) {
            createSchema(conn);
            seedInitialDataIfEmpty(conn);
        } catch (SQLException e) {
            System.err.println("[DatabaseManager] Schema initialization failed: " + e.getMessage());
            throw new RuntimeException("Failed to initialize database schema", e);
        }
    }

    private void createSchema(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            // 1. Base Components Table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS components (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    type TEXT NOT NULL,
                    brand TEXT NOT NULL,
                    name TEXT NOT NULL,
                    price REAL NOT NULL,
                    tdp_watts INTEGER NOT NULL
                );
            """);

            // 2. CPU Specs Table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS cpu_specs (
                    component_id INTEGER PRIMARY KEY,
                    socket TEXT NOT NULL,
                    cores INTEGER NOT NULL,
                    threads INTEGER NOT NULL,
                    base_clock REAL NOT NULL,
                    boost_clock REAL NOT NULL,
                    benchmark_score INTEGER NOT NULL,
                    FOREIGN KEY (component_id) REFERENCES components(id) ON DELETE CASCADE
                );
            """);

            // 3. GPU Specs Table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS gpu_specs (
                    component_id INTEGER PRIMARY KEY,
                    vram_gb INTEGER NOT NULL,
                    board_power_watts INTEGER NOT NULL,
                    recommended_psu_watts INTEGER NOT NULL,
                    fps_1080p REAL NOT NULL,
                    fps_1440p REAL NOT NULL,
                    fps_4k REAL NOT NULL,
                    FOREIGN KEY (component_id) REFERENCES components(id) ON DELETE CASCADE
                );
            """);

            // 4. Motherboard Specs Table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS motherboard_specs (
                    component_id INTEGER PRIMARY KEY,
                    socket TEXT NOT NULL,
                    ram_type TEXT NOT NULL,
                    FOREIGN KEY (component_id) REFERENCES components(id) ON DELETE CASCADE
                );
            """);

            // 5. RAM Specs Table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS ram_specs (
                    component_id INTEGER PRIMARY KEY,
                    ram_type TEXT NOT NULL,
                    capacity_gb INTEGER NOT NULL,
                    speed_mhz INTEGER NOT NULL,
                    FOREIGN KEY (component_id) REFERENCES components(id) ON DELETE CASCADE
                );
            """);

            // 6. PSU Specs Table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS psu_specs (
                    component_id INTEGER PRIMARY KEY,
                    wattage INTEGER NOT NULL,
                    efficiency_rating TEXT NOT NULL,
                    FOREIGN KEY (component_id) REFERENCES components(id) ON DELETE CASCADE
                );
            """);

            // 7. Price History Table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS price_history (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    component_id INTEGER NOT NULL,
                    price REAL NOT NULL,
                    source TEXT NOT NULL,
                    fetched_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (component_id) REFERENCES components(id) ON DELETE CASCADE
                );
            """);

            // 8. Builds Table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS builds (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT NOT NULL,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                );
            """);

            // 9. Build Items Table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS build_items (
                    build_id INTEGER NOT NULL,
                    component_id INTEGER NOT NULL,
                    PRIMARY KEY (build_id, component_id),
                    FOREIGN KEY (build_id) REFERENCES builds(id) ON DELETE CASCADE,
                    FOREIGN KEY (component_id) REFERENCES components(id) ON DELETE CASCADE
                );
            """);

            // 10. Performance Indexes
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_components_type ON components(type);");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_components_brand ON components(brand);");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_price_history_lookup ON price_history(component_id, fetched_at);");
        }
    }

    /**
     * Seeds starter hardware components if the database contains no components.
     */
    private void seedInitialDataIfEmpty(Connection conn) throws SQLException {
        try (Statement checkStmt = conn.createStatement();
             ResultSet rs = checkStmt.executeQuery("SELECT COUNT(*) AS count FROM components")) {
            if (rs.next() && rs.getInt("count") > 0) {
                return; // Already populated
            }
        }

        conn.setAutoCommit(false);
        try {
            // Seed 10 CPUs
            insertCpu(conn, "AMD", "Ryzen 5 5500", 89.99, 65, "AM4", 6, 12, 3.6, 4.2, 19500);
            insertCpu(conn, "AMD", "Ryzen 5 5600", 124.99, 65, "AM4", 6, 12, 3.5, 4.4, 21500);
            insertCpu(conn, "AMD", "Ryzen 5 5600X", 144.99, 65, "AM4", 6, 12, 3.7, 4.6, 22000);
            insertCpu(conn, "AMD", "Ryzen 5 7600", 189.99, 65, "AM5", 6, 12, 3.8, 5.1, 27500);
            insertCpu(conn, "AMD", "Ryzen 7 7700X", 289.99, 105, "AM5", 8, 16, 4.5, 5.4, 36000);
            insertCpu(conn, "AMD", "Ryzen 7 7800X3D", 369.99, 120, "AM5", 8, 16, 4.2, 5.0, 34500);
            insertCpu(conn, "Intel", "Core i3-12100F", 84.99, 58, "LGA1700", 4, 8, 3.3, 4.3, 14200);
            insertCpu(conn, "Intel", "Core i5-12400F", 119.99, 65, "LGA1700", 6, 12, 2.5, 4.4, 19800);
            insertCpu(conn, "Intel", "Core i5-13400F", 169.99, 65, "LGA1700", 10, 16, 2.5, 4.6, 25500);
            insertCpu(conn, "Intel", "Core i5-13600K", 269.99, 125, "LGA1700", 14, 20, 3.5, 5.1, 38200);
            insertCpu(conn, "Intel", "Core i5-14600K", 299.99, 125, "LGA1700", 14, 20, 3.5, 5.3, 39800);

            // Seed 10 GPUs
            insertGpu(conn, "AMD", "Radeon RX 6600", 199.99, 132, 8, 132, 450, 78.5, 52.0, 26.5);
            insertGpu(conn, "AMD", "Radeon RX 6650 XT", 229.99, 180, 8, 180, 500, 89.2, 59.8, 31.0);
            insertGpu(conn, "AMD", "Radeon RX 6700 XT", 329.99, 230, 12, 230, 650, 112.4, 79.1, 44.2);
            insertGpu(conn, "AMD", "Radeon RX 7600", 259.99, 165, 8, 165, 550, 91.0, 61.2, 32.5);
            insertGpu(conn, "AMD", "Radeon RX 7700 XT", 419.99, 245, 12, 245, 700, 135.0, 96.5, 54.0);
            insertGpu(conn, "NVIDIA", "GeForce RTX 3050", 179.99, 130, 8, 130, 450, 58.0, 38.5, 19.0);
            insertGpu(conn, "NVIDIA", "GeForce RTX 3060 12GB", 279.99, 170, 12, 170, 550, 85.0, 57.0, 29.5);
            insertGpu(conn, "NVIDIA", "GeForce RTX 4060", 299.99, 115, 8, 115, 500, 96.5, 66.0, 34.0);
            insertGpu(conn, "NVIDIA", "GeForce RTX 4060 Ti", 389.99, 160, 8, 160, 550, 118.0, 82.5, 43.5);
            insertGpu(conn, "Intel", "Arc A750", 199.99, 225, 8, 225, 550, 76.0, 53.5, 28.0);

            // Seed Motherboards
            insertMotherboard(conn, "MSI", "B550-A PRO", 109.99, 25, "AM4", "DDR4");
            insertMotherboard(conn, "ASUS", "TUF GAMING B650-PLUS", 199.99, 30, "AM5", "DDR5");
            insertMotherboard(conn, "MSI", "PRO B760-P WIFI DDR4", 139.99, 25, "LGA1700", "DDR4");
            insertMotherboard(conn, "Gigabyte", "B760 AORUS ELITE AX", 169.99, 30, "LGA1700", "DDR5");

            // Seed RAM
            insertRam(conn, "Corsair", "Vengeance LPX 16GB (2x8GB)", 39.99, 10, "DDR4", 16, 3200);
            insertRam(conn, "G.Skill", "Ripjaws V 32GB (2x16GB)", 64.99, 12, "DDR4", 32, 3600);
            insertRam(conn, "Kingston", "Fury Beast 16GB (2x8GB)", 59.99, 10, "DDR5", 16, 5600);
            insertRam(conn, "Corsair", "Vengeance 32GB (2x16GB)", 109.99, 15, "DDR5", 32, 6000);

            // Seed PSUs
            insertPsu(conn, "EVGA", "500 W1", 44.99, 0, 500, "80+ White");
            insertPsu(conn, "Corsair", "CX650M", 69.99, 0, 650, "80+ Bronze");
            insertPsu(conn, "Seasonic", "FOCUS GX-750", 109.99, 0, 750, "80+ Gold");
            insertPsu(conn, "Corsair", "RM850e", 119.99, 0, 850, "80+ Gold");

            conn.commit();
        } catch (SQLException ex) {
            conn.rollback();
            throw ex;
        } finally {
            conn.setAutoCommit(true);
        }
    }

    private int insertBaseComponent(Connection conn, String type, String brand, String name, double price, int tdp) throws SQLException {
        String sql = "INSERT INTO components (type, brand, name, price, tdp_watts) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, type);
            pstmt.setString(2, brand);
            pstmt.setString(3, name);
            pstmt.setDouble(4, price);
            pstmt.setInt(5, tdp);
            pstmt.executeUpdate();

            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) {
                    int id = rs.getInt(1);
                    recordInitialPrice(conn, id, price);
                    return id;
                }
            }
        }
        throw new SQLException("Failed to retrieve generated key for component: " + name);
    }

    private void recordInitialPrice(Connection conn, int componentId, double price) throws SQLException {
        String sql = "INSERT INTO price_history (component_id, price, source) VALUES (?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, componentId);
            pstmt.setDouble(2, price);
            pstmt.setString(3, "SEED_DATA");
            pstmt.executeUpdate();
        }
    }

    private void insertCpu(Connection conn, String brand, String name, double price, int tdp,
                           String socket, int cores, int threads, double base, double boost, int score) throws SQLException {
        int id = insertBaseComponent(conn, "CPU", brand, name, price, tdp);
        String sql = "INSERT INTO cpu_specs (component_id, socket, cores, threads, base_clock, boost_clock, benchmark_score) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            pstmt.setString(2, socket);
            pstmt.setInt(3, cores);
            pstmt.setInt(4, threads);
            pstmt.setDouble(5, base);
            pstmt.setDouble(6, boost);
            pstmt.setInt(7, score);
            pstmt.executeUpdate();
        }
    }

    private void insertGpu(Connection conn, String brand, String name, double price, int tdp,
                           int vram, int boardPower, int recPsu, double fps1080p, double fps1440p, double fps4k) throws SQLException {
        int id = insertBaseComponent(conn, "GPU", brand, name, price, tdp);
        String sql = "INSERT INTO gpu_specs (component_id, vram_gb, board_power_watts, recommended_psu_watts, fps_1080p, fps_1440p, fps_4k) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            pstmt.setInt(2, vram);
            pstmt.setInt(3, boardPower);
            pstmt.setInt(4, recPsu);
            pstmt.setDouble(5, fps1080p);
            pstmt.setDouble(6, fps1440p);
            pstmt.setDouble(7, fps4k);
            pstmt.executeUpdate();
        }
    }

    private void insertMotherboard(Connection conn, String brand, String name, double price, int tdp,
                                  String socket, String ramType) throws SQLException {
        int id = insertBaseComponent(conn, "MOTHERBOARD", brand, name, price, tdp);
        String sql = "INSERT INTO motherboard_specs (component_id, socket, ram_type) VALUES (?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            pstmt.setString(2, socket);
            pstmt.setString(3, ramType);
            pstmt.executeUpdate();
        }
    }

    private void insertRam(Connection conn, String brand, String name, double price, int tdp,
                          String ramType, int capacity, int speed) throws SQLException {
        int id = insertBaseComponent(conn, "RAM", brand, name, price, tdp);
        String sql = "INSERT INTO ram_specs (component_id, ram_type, capacity_gb, speed_mhz) VALUES (?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            pstmt.setString(2, ramType);
            pstmt.setInt(3, capacity);
            pstmt.setInt(4, speed);
            pstmt.executeUpdate();
        }
    }

    private void insertPsu(Connection conn, String brand, String name, double price, int tdp,
                          int wattage, String rating) throws SQLException {
        int id = insertBaseComponent(conn, "PSU", brand, name, price, tdp);
        String sql = "INSERT INTO psu_specs (component_id, wattage, efficiency_rating) VALUES (?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            pstmt.setInt(2, wattage);
            pstmt.setString(3, rating);
            pstmt.executeUpdate();
        }
    }
}
