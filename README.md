# PC Hardware Benchmark & Build Analyzer

[![Java 21](https://img.shields.io/badge/Java-21-orange.svg)](https://www.oracle.com/java/technologies/downloads/#java21)
[![JavaFX 21](https://img.shields.io/badge/JavaFX-21.0.2-blue.svg)](https://openjfx.io/)
[![SQLite](https://img.shields.io/badge/Database-SQLite-lightgrey.svg)](https://www.sqlite.org/)
[![Jackson JSON](https://img.shields.io/badge/JSON-Jackson--Databind-green.svg)](https://github.com/FasterXML/jackson)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

A feature-rich desktop application built with **Java 21** and **JavaFX** for analyzing PC hardware components, running real-time build compatibility diagnostics, comparing multi-tier benchmarks & price-to-performance efficiency, tracking price histories in an **SQLite** database, and synchronizing market pricing using **HTTP JSON APIs** and **Jackson JSON parsing**.

Repository Link: [https://github.com/ziggyzu/PC-Hardware-Benchmark-Build-Analyzer](https://github.com/ziggyzu/PC-Hardware-Benchmark-Build-Analyzer)

---

## 🚀 Key Features & Capabilities

### 1. ⚡ Live HTTP & Local JSON Market Data Synchronization
* **Live Web Endpoint Integration**: Fetches real-time market pricing over HTTP GET using Java 11+ `java.net.http.HttpClient` targeting remote pricing feeds:
  - Remote API Endpoint: [`data/hardware-pricing.json`](https://github.com/ziggyzu/PC-Hardware-Benchmark-Build-Analyzer/blob/master/data/hardware-pricing.json)
* **Jackson Tree Node Parsing**: Employs Jackson `ObjectMapper` and `JsonNode` tree model parsing to process hardware JSON payloads.
* **Manual Local JSON File Parsing**: Import and parse custom manual `.json` files from disk via an interactive file chooser ([`data/hardware-pricing.json`](https://github.com/ziggyzu/PC-Hardware-Benchmark-Build-Analyzer/blob/master/data/hardware-pricing.json)).
* **Automatic Database Insertion & Updates**:
  - Updates prices for existing database components when market price shifts occur.
  - Automatically instantiates and inserts **brand-new hardware items** into SQLite if not currently in the database catalog.
  - Logs timestamped price changes into the `price_history` database table.
* **Offline Resiliency & Fallback Payload**: Automatically falls back to an embedded JSON payload if network connections time out or fail.
* **Protocol & Parsing Demonstration Popup**: Displays an interactive GUI popup containing HTTP request headers, response status codes, raw JSON text payloads, and step-by-step Jackson parsing execution logs.
* **Source Implementation**: [`HardwareSyncService.java`](https://github.com/ziggyzu/PC-Hardware-Benchmark-Build-Analyzer/blob/master/src/main/java/com/pcanalyzer/service/HardwareSyncService.java)

---

### 2. 🔍 Hardware Catalog Browser & Price History Tracking
* **Multi-Category Hardware Filter**: Search and filter by component type: **CPU**, **GPU**, **Motherboard**, **RAM**, and **Power Supply (PSU)**.
* **Real-time Search & Instant Search**: Filter catalog entries instantaneously by brand or model name.
* **Price History Charting**: Visualizes historical price trends recorded over time in SQLite for every hardware part.
* **Admin Privilege Authorization**: Unlock elevated administrative rights (`admin123`) to manually create new hardware components via custom JavaFX dialogs.
* **Source Implementation**:
  - FXML View: [`browse-view.fxml`](https://github.com/ziggyzu/PC-Hardware-Benchmark-Build-Analyzer/blob/master/src/main/resources/com/pcanalyzer/view/browse-view.fxml)
  - Controller: [`BrowseController.java`](https://github.com/ziggyzu/PC-Hardware-Benchmark-Build-Analyzer/blob/master/src/main/java/com/pcanalyzer/ui/BrowseController.java)
  - Dialog FXML: [`add-component-dialog.fxml`](https://github.com/ziggyzu/PC-Hardware-Benchmark-Build-Analyzer/blob/master/src/main/resources/com/pcanalyzer/view/add-component-dialog.fxml)

---

### 3. 🛠️ PC Build Configurator & Real-Time Compatibility Diagnostics
* **Slot-based PC Builder**: Assemble full desktop configurations by selecting CPUs, GPUs, Motherboards, RAM kits, and Power Supplies.
* **Automated Rule Engine Diagnostics**: Evaluates builds against hardware standard constraints:
  - **Socket Compatibility Check**: Validates CPU socket alignment against Motherboard socket standards (e.g. `AM4`, `AM5`, `LGA1700`).
  - **Memory Standard Check**: Ensures Motherboard RAM slots match RAM standard generations (e.g. `DDR4` vs `DDR5`).
  - **Power Supply (PSU) Headroom Check**: Calculates cumulative system TDP (Thermal Design Power), verifies recommended GPU PSU wattage, and ensures a safe +20% power headroom margin.
* **Save & Load Configurations**: Store completed custom builds directly into SQLite tables (`builds` and `build_items`) for later retrieval.
* **Source Implementation**:
  - FXML View: [`build-panel.fxml`](https://github.com/ziggyzu/PC-Hardware-Benchmark-Build-Analyzer/blob/master/src/main/resources/com/pcanalyzer/view/build-panel.fxml)
  - Controller: [`BuildPanelController.java`](https://github.com/ziggyzu/PC-Hardware-Benchmark-Build-Analyzer/blob/master/src/main/java/com/pcanalyzer/ui/BuildPanelController.java)
  - Diagnostic Engine: [`CompatibilityChecker.java`](https://github.com/ziggyzu/PC-Hardware-Benchmark-Build-Analyzer/blob/master/src/main/java/com/pcanalyzer/service/CompatibilityChecker.java)

---

### 4. 📊 Performance Comparisons & Price-to-Performance Value Analysis
* **Multi-Resolution Gaming FPS Charts**: Interactive bar charts comparing GPU frame rates across three resolution settings: **1080p (FHD)**, **1440p (QHD)**, and **4K (UHD)**.
* **Processor Benchmark Score Charts**: Compare multi-threaded CPU benchmark scores (PassMark rating scale).
* **Value Efficiency Analysis**:
  - **GPU Cost per Frame**: $\text{Cost per FPS} = \frac{\text{Price}}{\text{FPS}}$ (Lower is Better).
  - **CPU Points per Dollar**: $\text{Points per USD} = \frac{\text{Benchmark Score}}{\text{Price}}$ (Higher is Better).
* **Head-to-Head Specification Comparison**: Side-by-side spec grid with color-coded winner highlights (green winner / red loser).
* **Source Implementation**:
  - FXML View: [`comparison-view.fxml`](https://github.com/ziggyzu/PC-Hardware-Benchmark-Build-Analyzer/blob/master/src/main/resources/com/pcanalyzer/view/comparison-view.fxml)
  - Controller: [`ComparisonController.java`](https://github.com/ziggyzu/PC-Hardware-Benchmark-Build-Analyzer/blob/master/src/main/java/com/pcanalyzer/ui/ComparisonController.java)
  - Analytics Calculator: [`BuildAnalyzer.java`](https://github.com/ziggyzu/PC-Hardware-Benchmark-Build-Analyzer/blob/master/src/main/java/com/pcanalyzer/service/BuildAnalyzer.java)

---

### 5. 🗄️ Relational Database & Concurrency Architecture
* **SQLite Relational Schema**: 9 indexed database tables managed via standard JDBC:
  - Core Tables: [`components`](https://github.com/ziggyzu/PC-Hardware-Benchmark-Build-Analyzer/blob/master/src/main/java/com/pcanalyzer/db/DatabaseManager.java#L56), [`cpu_specs`](https://github.com/ziggyzu/PC-Hardware-Benchmark-Build-Analyzer/blob/master/src/main/java/com/pcanalyzer/db/DatabaseManager.java#L68), [`gpu_specs`](https://github.com/ziggyzu/PC-Hardware-Benchmark-Build-Analyzer/blob/master/src/main/java/com/pcanalyzer/db/DatabaseManager.java#L82), [`motherboard_specs`](https://github.com/ziggyzu/PC-Hardware-Benchmark-Build-Analyzer/blob/master/src/main/java/com/pcanalyzer/db/DatabaseManager.java#L96), [`ram_specs`](https://github.com/ziggyzu/PC-Hardware-Benchmark-Build-Analyzer/blob/master/src/main/java/com/pcanalyzer/db/DatabaseManager.java#L106), [`psu_specs`](https://github.com/ziggyzu/PC-Hardware-Benchmark-Build-Analyzer/blob/master/src/main/java/com/pcanalyzer/db/DatabaseManager.java#L117), [`price_history`](https://github.com/ziggyzu/PC-Hardware-Benchmark-Build-Analyzer/blob/master/src/main/java/com/pcanalyzer/db/DatabaseManager.java#L127), [`builds`](https://github.com/ziggyzu/PC-Hardware-Benchmark-Build-Analyzer/blob/master/src/main/java/com/pcanalyzer/db/DatabaseManager.java#L139), [`build_items`](https://github.com/ziggyzu/PC-Hardware-Benchmark-Build-Analyzer/blob/master/src/main/java/com/pcanalyzer/db/DatabaseManager.java#L148).
* **DAO Pattern Implementation**: Clean separation between database querying and domain models:
  - [`ComponentDao.java`](https://github.com/ziggyzu/PC-Hardware-Benchmark-Build-Analyzer/blob/master/src/main/java/com/pcanalyzer/db/ComponentDao.java) / [`SqliteComponentDao.java`](https://github.com/ziggyzu/PC-Hardware-Benchmark-Build-Analyzer/blob/master/src/main/java/com/pcanalyzer/db/SqliteComponentDao.java)
  - [`PriceHistoryDao.java`](https://github.com/ziggyzu/PC-Hardware-Benchmark-Build-Analyzer/blob/master/src/main/java/com/pcanalyzer/db/PriceHistoryDao.java) / [`SqlitePriceHistoryDao.java`](https://github.com/ziggyzu/PC-Hardware-Benchmark-Build-Analyzer/blob/master/src/main/java/com/pcanalyzer/db/SqlitePriceHistoryDao.java)
* **Asynchronous Thread Pool Manager**: Dedicated multi-worker executor service preventing UI freezes during database queries and network calls.
  - Implementation: [`ThreadPoolManager.java`](https://github.com/ziggyzu/PC-Hardware-Benchmark-Build-Analyzer/blob/master/src/main/java/com/pcanalyzer/util/ThreadPoolManager.java)

---

## 🛠️ Project Structure

```text
PC-Hardware-Benchmark-Build-Analyzer/
├── data/
│   └── hardware-pricing.json              # Local JSON hardware pricing dataset
├── src/
│   ├── main/
│   │   ├── java/com/pcanalyzer/
│   │   │   ├── App.java                   # Main JavaFX application launcher
│   │   │   ├── db/                        # Database Manager & SQLite DAOs
│   │   │   │   ├── DatabaseManager.java
│   │   │   │   ├── ComponentDao.java
│   │   │   │   ├── SqliteComponentDao.java
│   │   │   │   ├── PriceHistoryDao.java
│   │   │   │   └── SqlitePriceHistoryDao.java
│   │   │   ├── model/                     # Domain models (CPU, GPU, RAM, PSU, Motherboard)
│   │   │   │   ├── Component.java
│   │   │   │   ├── ComponentType.java
│   │   │   │   ├── Cpu.java
│   │   │   │   ├── Gpu.java
│   │   │   │   ├── Motherboard.java
│   │   │   │   ├── Ram.java
│   │   │   │   └── Psu.java
│   │   │   ├── service/                   # Business logic services
│   │   │   │   ├── BuildAnalyzer.java
│   │   │   │   ├── CompatibilityChecker.java
│   │   │   │   └── HardwareSyncService.java
│   │   │   ├── ui/                        # JavaFX UI Controllers
│   │   │   │   ├── MainController.java
│   │   │   │   ├── BrowseController.java
│   │   │   │   ├── BuildPanelController.java
│   │   │   │   ├── ComparisonController.java
│   │   │   │   └── AddComponentDialogController.java
│   │   │   └── util/                      # Multi-threading utilities
│   │   │       └── ThreadPoolManager.java
│   │   └── resources/com/pcanalyzer/
│   │       ├── css/style.css              # Custom JavaFX styling & themes
│   │       ├── data/hardware-pricing.json # Embedded fallback JSON resource dataset
│   │       └── view/                      # FXML layout views
│   │           ├── main-view.fxml
│   │           ├── browse-view.fxml
│   │           ├── build-panel.fxml
│   │           ├── comparison-view.fxml
│   │           └── add-component-dialog.fxml
│   └── test/java/com/pcanalyzer/          # JUnit 5 unit test suite
│       ├── db/SqliteComponentDaoTest.java
│       ├── model/BuildModelTest.java
│       ├── service/CompatibilityCheckerTest.java
│       ├── service/HardwareSyncServiceTest.java
│       ├── ui/ComparisonControllerTest.java
│       └── util/ThreadPoolManagerTest.java
├── pom.xml                                # Maven build configuration
└── run.bat                                # Application launch script
```

---

## 💻 Tech Stack & Prerequisites

* **Language**: Java 21 (JDK 21)
* **UI Framework**: JavaFX 21.0.2 (`javafx-controls`, `javafx-fxml`)
* **Database Driver**: SQLite JDBC (`3.45.2.0`)
* **JSON Parser**: Jackson Databind (`2.17.0`)
* **Testing Framework**: JUnit 5 (`5.10.2`)
* **Build Tool**: Apache Maven

---

## ⚙️ How to Build and Run

### 1. Clone the Repository
```bash
git clone https://github.com/ziggyzu/PC-Hardware-Benchmark-Build-Analyzer.git
cd PC-Hardware-Benchmark-Build-Analyzer
```

### 2. Run the Application
Using the Maven Wrapper:
```bash
# Windows
.\mvnw.cmd javafx:run

# Linux / macOS
./mvnw javafx:run
```
Alternatively, on Windows double-click or run [`run.bat`](https://github.com/ziggyzu/PC-Hardware-Benchmark-Build-Analyzer/blob/master/run.bat).

### 3. Run Automated Unit Tests
```bash
# Windows
.\mvnw.cmd test

# Linux / macOS
./mvnw test
```

---

## 📄 License

This project is open-source and available under the [MIT License](LICENSE).
