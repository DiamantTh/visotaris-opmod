package systems.diath.noopmod.api;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import systems.diath.noopmod.NoOpConst;
import systems.diath.noopmod.NoOpLogger;
import systems.diath.noopmod.config.ConfigManager;
import systems.diath.noopmod.model.MarketPrice;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * HTTP-Client für die OPSUCHT-Marktpreis-API.
 *
 * Echtes API-Format (verifiziert):
 * <pre>
 * {
 *   "Holz": {
 *     "ACACIA_LEAVES": [
 *       {"orderSide":"BUY",  "activeOrders":62, "price":49.5},
 *       {"orderSide":"SELL", "activeOrders":11, "price":3.8}
 *     ],
 *     ...
 *   },
 *   "Erze": { ... },
 *   ...
 * }
 * </pre>
 * Root ist ein Objekt (Kategorie → Items), Item-Keys sind UPPERCASE.
 * BUY und SELL sind separate Einträge im Array je Item.
 */
public final class MarketApiClient {

    private static final String ENDPOINT   = "https://api.opsucht.net/market/prices";
    private static final int    TIMEOUT_MS = 10_000;
    private static final Gson   GSON       = new Gson();

    private final ConfigManager configManager;

    public MarketApiClient(ConfigManager configManager) {
        this.configManager = configManager;
    }

    public List<MarketPrice> fetchPrices() throws IOException {
        HttpClient client   = NoOpConst.buildHttpClient(configManager.getConfig());
        HttpRequest request = buildRequest(ENDPOINT);

        HttpResponse<InputStream> response;
        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Markt-API Request unterbrochen", e);
        }

        int status = response.statusCode();
        if (status != 200) {
            response.body().close();
            throw new IOException("Markt-API Status " + status);
        }

        try (InputStream is = response.body();
             InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {

            JsonElement rootEl = GSON.fromJson(reader, JsonElement.class);

            if (!rootEl.isJsonObject()) {
                NoOpLogger.warn("Markt-API: unerwartetes Root-Element (kein Object).");
                return Collections.emptyList();
            }

            JsonObject root = rootEl.getAsJsonObject();
            List<MarketPrice> result = new ArrayList<>(512);

            // Äußere Kategorie-Ebene ("Holz", "Erze", ...)
            for (Map.Entry<String, JsonElement> catEntry : root.entrySet()) {
                if (!catEntry.getValue().isJsonObject()) continue;
                JsonObject category = catEntry.getValue().getAsJsonObject();

                // Item-Ebene ("ACACIA_LEAVES", "DIAMOND", ...)
                for (Map.Entry<String, JsonElement> itemEntry : category.entrySet()) {
                    String itemKey = itemEntry.getKey().toLowerCase();   // → acacia_leaves
                    if (!itemEntry.getValue().isJsonArray()) continue;
                    JsonArray orders = itemEntry.getValue().getAsJsonArray();

                    double buyPrice  = 0;
                    double sellPrice = 0;

                    for (JsonElement orderEl : orders) {
                        if (!orderEl.isJsonObject()) continue;
                        JsonObject order = orderEl.getAsJsonObject();
                        String side  = getStringOrNull(order, "orderSide");
                        double price = getDouble(order, "price");

                        if ("BUY".equalsIgnoreCase(side))  buyPrice  = price;
                        else if ("SELL".equalsIgnoreCase(side)) sellPrice = price;
                    }

                    if (buyPrice > 0 || sellPrice > 0) {
                        result.add(new MarketPrice(itemKey, buyPrice, sellPrice));
                    }
                }
            }

            NoOpLogger.debug("Markt-API: {} Preiseinträge geladen.", result.size());
            return result;
        }
    }

    private HttpRequest buildRequest(String url) {
        var cfg = configManager.getConfig();
        HttpRequest.Builder req = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .timeout(Duration.ofMillis(TIMEOUT_MS))
            .GET()
            .header("Accept", "application/json")
            .header("User-Agent", NoOpConst.buildUserAgent(cfg.customUserAgent));
        if (cfg.apiKey != null && !cfg.apiKey.isBlank()) {
            req.header("Authorization", "Bearer " + cfg.apiKey.strip());
        }
        return req.build();
    }

    private static String getStringOrNull(JsonObject obj, String key) {
        JsonElement el = obj.get(key);
        return (el != null && el.isJsonPrimitive()) ? el.getAsString() : null;
    }

    private static double getDouble(JsonObject obj, String key) {
        JsonElement el = obj.get(key);
        return (el != null && el.isJsonPrimitive()) ? el.getAsDouble() : 0.0;
    }
}