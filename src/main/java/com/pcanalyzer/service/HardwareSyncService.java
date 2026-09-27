package com.pcanalyzer.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pcanalyzer.db.ComponentDao;
import com.pcanalyzer.db.PriceHistoryDao;
import com.pcanalyzer.model.*;
import com.pcanalyzer.util.ThreadPoolManager;
import javafx.application.Platform;
import javafx.concurrent.Task;

import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.time.Duration;
import java.util.List;
import java.util.function.Consumer;

// Fetches live component prices from the web or local JSON files and saves them to the database
public class HardwareSyncService {

    // Where to look online for updated pricing
    private static final String DEFAULT_PRICING_API_URL = "https://raw.githubusercontent.com/ziggyzu/PC-Hardware-Benchmark-Build-Analyzer/master/data/hardware-pricing.json";
    
    // Backup price list in case the computer is offline
    private static final String FALLBACK_JSON = """
        [
            {"brand": "AMD", "name": "Ryzen 5 5600", "price": 119.99, "source": "Newegg_Sync"},
            {"brand": "AMD", "name": "Ryzen 7 7700X", "price": 279.99, "source": "Amazon_Sync"},
            {"brand": "AMD", "name": "Ryzen 7 7800X3D", "price": 359.99, "source": "MicroCenter_Sync"},
            {"brand": "NVIDIA", "name": "GeForce RTX 4060", "price": 289.99, "source": "BestBuy_Sync"},
            {"brand": "NVIDIA", "name": "GeForce RTX 4060 Ti", "price": 379.99, "source": "Amazon_Sync"},
            {"brand": "AMD", "name": "Radeon RX 6700 XT", "price": 319.99, "source": "Newegg_Sync"}
        ]
        """;

    private final ComponentDao componentDao;
    private final PriceHistoryDao priceHistoryDao;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    // Set up our database connections and HTTP client
    public HardwareSyncService(ComponentDao componentDao, PriceHistoryDao priceHistoryDao) {
        this.componentDao = componentDao;
        this.priceHistoryDao = priceHistoryDao;
        this.objectMapper = new ObjectMapper();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    // Results returned after finishing a sync
    public record SyncResult(
            int updatedCount,
            String source,
            String message,
            boolean success,
            String rawJson,
            String httpDetails
    ) {
        public SyncResult(int updatedCount, String source, String message, boolean success) {
            this(updatedCount, source, message, success, "", "");
        }
    }

    // Default sync method
    public SyncResult performSync() throws Exception {
        return performSyncWithDetails();
    }

    // Download price data over HTTP, read JSON via Jackson, and update database components
    public SyncResult performSyncWithDetails() throws Exception {
        String threadName = Thread.currentThread().getName();
        String jsonContent;
        String sourceName;
        StringBuilder httpLog = new StringBuilder();

        httpLog.append("HTTP Request: GET ").append(DEFAULT_PRICING_API_URL).append("\n");
        httpLog.append("Headers: Accept=application/json\n");
        httpLog.append("Timeout: 3 seconds\n");

        // Try downloading latest prices from the internet
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(DEFAULT_PRICING_API_URL))
                    .timeout(Duration.ofSeconds(3))
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            httpLog.append("Response Status: ").append(response.statusCode())
                   .append(response.statusCode() == 200 ? " OK\n" : " (Non-200 Response)\n");
            httpLog.append("Content-Type: ").append(response.headers().firstValue("content-type").orElse("application/json")).append("\n");

            if (response.statusCode() == 200 && response.body() != null && !response.body().isBlank()) {
                jsonContent = response.body();
                sourceName = "Live_HTTP_API";
            } else {
                jsonContent = FALLBACK_JSON;
                sourceName = "Offline_JSON_Feed";
                httpLog.append("Warning: HTTP Response empty or non-200. Used offline fallback.\n");
            }
        } catch (Exception e) {
            // Use our backup prices if the internet request fails or times out
            jsonContent = FALLBACK_JSON;
            sourceName = "Local_JSON_Payload";
            httpLog.append("HTTP Request Exception: ").append(e.getClass().getSimpleName())
                   .append(" - ").append(e.getMessage()).append("\n")
                   .append("Fallback to embedded offline JSON payload.\n");
        }

        return parseAndApplyJson(jsonContent, sourceName, httpLog.toString(), threadName);
    }

    // Read and parse JSON from a local disk file
    public SyncResult performSyncFromLocalFile(File file) throws Exception {
        if (file == null || !file.exists()) {
            throw new IllegalArgumentException("Target JSON file does not exist.");
        }

        String threadName = Thread.currentThread().getName();
        String jsonContent = Files.readString(file.toPath());
        String sourceName = "Local_File: " + file.getName();
        String logDetails = "Source: Local File [" + file.getAbsolutePath() + "]\nFile Size: " + file.length() + " bytes";

        return parseAndApplyJson(jsonContent, sourceName, logDetails, threadName);
    }

    // Core method that uses Jackson ObjectMapper to parse JSON and update SQLite
    public SyncResult parseAndApplyJson(String jsonContent, String sourceName, String logDetails, String threadName) throws Exception {
        // Parse through the JSON payload using Jackson
        JsonNode rootNode = objectMapper.readTree(jsonContent);
        int updatedCount = 0;

        if (rootNode.isArray()) {
            List<Component> allComponents = componentDao.findAll();

            for (JsonNode itemNode : rootNode) {
                String brand = itemNode.path("brand").asText();
                String name = itemNode.path("name").asText();
                double newPrice = itemNode.path("price").asDouble();
                String itemSource = itemNode.path("source").asText(sourceName);

                boolean found = false;
                for (Component comp : allComponents) {
                    if (comp.getBrand().equalsIgnoreCase(brand) && comp.getName().equalsIgnoreCase(name)) {
                        found = true;
                        if (Math.abs(comp.getPrice() - newPrice) > 0.01) {
                            comp.setPrice(newPrice);
                            componentDao.save(comp);
                            if (priceHistoryDao != null && comp.getId() != null) {
                                priceHistoryDao.recordPrice(comp.getId(), newPrice, itemSource);
                            }
                            updatedCount++;
                        }
                        break;
                    }
                }

                // If component does not exist in database, create and insert it as a new component
                if (!found && !brand.isBlank() && !name.isBlank()) {
                    Component newComp = createComponentFromJsonNode(itemNode, brand, name, newPrice);
                    if (newComp != null) {
                        Component saved = componentDao.save(newComp);
                        if (priceHistoryDao != null && saved != null && saved.getId() != null) {
                            priceHistoryDao.recordPrice(saved.getId(), newPrice, itemSource);
                        }
                        allComponents.add(saved);
                        updatedCount++;
                    }
                }
            }
        }

        String message = "Successfully synced " + updatedCount + " components via " + sourceName + " on [" + threadName + "]";
        return new SyncResult(updatedCount, sourceName, message, true, jsonContent, logDetails);
    }

    // Helper to construct a new Component instance from JSON attributes
    private Component createComponentFromJsonNode(JsonNode node, String brand, String name, double price) {
        String typeStr = node.path("type").asText("CPU").toUpperCase();
        int tdp = node.path("tdp_watts").asInt(65);

        return switch (typeStr) {
            case "GPU" -> new Gpu(
                    null, brand, name, price, tdp,
                    node.path("vram_gb").asInt(8),
                    node.path("board_power_watts").asInt(180),
                    node.path("recommended_psu_watts").asInt(550),
                    node.path("fps_1080p").asDouble(90.0),
                    node.path("fps_1440p").asDouble(60.0),
                    node.path("fps_4k").asDouble(30.0)
            );
            case "MOTHERBOARD" -> new Motherboard(
                    null, brand, name, price, tdp,
                    node.path("socket").asText("AM4"),
                    node.path("ram_type").asText("DDR4")
            );
            case "RAM" -> new Ram(
                    null, brand, name, price, tdp,
                    node.path("ram_type").asText("DDR4"),
                    node.path("capacity_gb").asInt(16),
                    node.path("speed_mhz").asInt(3200)
            );
            case "PSU" -> new Psu(
                    null, brand, name, price, tdp,
                    node.path("wattage").asInt(650),
                    node.path("efficiency_rating").asText("80+ Gold")
            );
            default -> new Cpu(
                    null, brand, name, price, tdp,
                    node.path("socket").asText("AM4"),
                    node.path("cores").asInt(6),
                    node.path("threads").asInt(12),
                    node.path("base_clock").asDouble(3.5),
                    node.path("boost_clock").asDouble(4.4),
                    node.path("benchmark_score").asInt(20000)
            );
        };
    }

    // Wrap the sync logic into a JavaFX background task
    public Task<SyncResult> createSyncTask() {
        return new Task<>() {
            @Override
            protected SyncResult call() throws Exception {
                updateMessage("Fetching live market data on [" + Thread.currentThread().getName() + "]...");
                return performSyncWithDetails();
            }
        };
    }

    // Run HTTP sync on a background thread
    public void syncAsynchronously(Consumer<SyncResult> onComplete, Consumer<Throwable> onError) {
        ThreadPoolManager.getInstance().execute(() -> {
            try {
                SyncResult result = performSyncWithDetails();
                if (onComplete != null) {
                    dispatchToUi(() -> onComplete.accept(result));
                }
            } catch (Throwable t) {
                if (onError != null) {
                    dispatchToUi(() -> onError.accept(t));
                }
            }
        });
    }

    // Run local JSON file sync on a background thread
    public void syncLocalFileAsynchronously(File file, Consumer<SyncResult> onComplete, Consumer<Throwable> onError) {
        ThreadPoolManager.getInstance().execute(() -> {
            try {
                SyncResult result = performSyncFromLocalFile(file);
                if (onComplete != null) {
                    dispatchToUi(() -> onComplete.accept(result));
                }
            } catch (Throwable t) {
                if (onError != null) {
                    dispatchToUi(() -> onError.accept(t));
                }
            }
        });
    }

    // Safely send results back to the main window
    private void dispatchToUi(Runnable runnable) {
        try {
            Platform.runLater(runnable);
        } catch (IllegalStateException e) {
            runnable.run();
        }
    }
}

