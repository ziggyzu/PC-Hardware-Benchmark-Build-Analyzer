package com.pcanalyzer.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pcanalyzer.db.ComponentDao;
import com.pcanalyzer.db.PriceHistoryDao;
import com.pcanalyzer.model.Component;
import com.pcanalyzer.util.ThreadPoolManager;
import javafx.application.Platform;
import javafx.concurrent.Task;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.function.Consumer;

// Fetches live component prices from the web and saves them to the database
public class HardwareSyncService {

    // Where to look online for updated pricing
    private static final String DEFAULT_PRICING_API_URL = "https://raw.githubusercontent.com/ziggyzu/PC-Hardware-Benchmark-Build-Analyzer/master/data/pricing-feed.json";
    
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
                .connectTimeout(Duration.ofSeconds(1))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    // Results returned after finishing a sync
    public record SyncResult(int updatedCount, String source, String message, boolean success) {}

    // Download price data, read the JSON, and update matching parts in the database
    public SyncResult performSync() throws Exception {
        String threadName = Thread.currentThread().getName();
        String jsonContent;
        String sourceName;

        // Try downloading latest prices from the internet
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(DEFAULT_PRICING_API_URL))
                    .timeout(Duration.ofSeconds(1))
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200 && response.body() != null && !response.body().isBlank()) {
                jsonContent = response.body();
                sourceName = "Live_HTTP_API";
            } else {
                jsonContent = FALLBACK_JSON;
                sourceName = "Offline_JSON_Feed";
            }
        } catch (Exception e) {
            // Use our backup prices if the internet request fails or times out
            jsonContent = FALLBACK_JSON;
            sourceName = "Local_JSON_Payload";
        }

        // Parse through the JSON list using Jackson
        JsonNode rootNode = objectMapper.readTree(jsonContent);
        int updatedCount = 0;

        // Loop through each part in the JSON and update its price in the database
        if (rootNode.isArray()) {
            List<Component> allComponents = componentDao.findAll();

            for (JsonNode itemNode : rootNode) {
                String brand = itemNode.path("brand").asText();
                String name = itemNode.path("name").asText();
                double newPrice = itemNode.path("price").asDouble();
                String itemSource = itemNode.path("source").asText(sourceName);

                for (Component comp : allComponents) {
                    if (comp.getBrand().equalsIgnoreCase(brand) && comp.getName().equalsIgnoreCase(name)) {
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
            }
        }

        // Return a summary of how many parts were updated
        return new SyncResult(updatedCount, sourceName,
                "Successfully synced " + updatedCount + " components via " + sourceName + " on [" + threadName + "]",
                true);
    }

    // Wrap the sync logic into a JavaFX background task
    public Task<SyncResult> createSyncTask() {
        return new Task<>() {
            @Override
            protected SyncResult call() throws Exception {
                updateMessage("Fetching live market data on [" + Thread.currentThread().getName() + "]...");
                return performSync();
            }
        };
    }

    // Run the sync on a background thread so the user interface never freezes
    public void syncAsynchronously(Consumer<SyncResult> onComplete, Consumer<Throwable> onError) {
        ThreadPoolManager.getInstance().execute(() -> {
            try {
                SyncResult result = performSync();
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
