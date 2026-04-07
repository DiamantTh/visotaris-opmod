package systems.diath.visotaris.api;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import okhttp3.Cache;
import okhttp3.CacheControl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import systems.diath.visotaris.VisotarisConst;
import systems.diath.visotaris.VisotarisLogger;
import systems.diath.visotaris.config.ConfigManager;
import systems.diath.visotaris.model.MarketPrice;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
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
    private static final Gson   GSON       = new Gson();

    private final ConfigManager configManager;
    private final OkHttpClient  httpClient;

    public MarketApiClient(ConfigManager configManager) {
        this.configManager = configManager;
        this.httpClient = VisotarisConst.buildOkHttpClient(configManager.getConfig())
            .newBuilder()
            .cache(new Cache(VisotarisConst.getCacheDir("market"), 5L * 1024 * 1024))
            .addNetworkInterceptor(chain -> {
                Response resp = chain.proceed(chain.request());
                // Antwort für Offline-Fallback immer auf Disk speichern
                return resp.newBuilder()
                    .header("Cache-Control", "public, max-age=86400")
                    .build();
            })
            .build();
    }

    public List<MarketPrice> fetchPrices() throws IOException {
        Request liveReq = new Request.Builder()
            .url(ENDPOINT)
            .cacheControl(CacheControl.FORCE_NETWORK)
            .build();
        try (Response response = httpClient.newCall(liveReq).execute()) {
            int status = response.code();
            if (status != 200) throw new IOException("Markt-API Status " + status);
            ResponseBody body = response.body();
            if (body == null) throw new IOException("Markt-API: leerer Response-Body");
            return parseBodyStream(body);
        } catch (IOException networkEx) {
            VisotarisLogger.warn("Markt-API offline \u2013 verwende Disk-Cache: {}", networkEx.getMessage());
            Request cacheReq = liveReq.newBuilder()
                .cacheControl(CacheControl.FORCE_CACHE)
                .build();
            try (Response cached = httpClient.newCall(cacheReq).execute()) {
                if (cached.code() == 504) throw networkEx;  // Kein Cache vorhanden
                VisotarisLogger.info("Markt-API: Disk-Cache verwendet.");
                ResponseBody body = cached.body();
                if (body == null) throw new IOException("Markt-API: leerer Cache-Body");
                return parseBodyStream(body);
            }
        }
    }

    private List<MarketPrice> parseBodyStream(ResponseBody body) throws IOException {
        try (InputStream is = body.byteStream();
             InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
            JsonElement rootEl = GSON.fromJson(reader, JsonElement.class);

            if (!rootEl.isJsonObject()) {
                VisotarisLogger.warn("Markt-API: unerwartetes Root-Element (kein Object).");
                return Collections.emptyList();
            }

            JsonObject root = rootEl.getAsJsonObject();
            List<MarketPrice> result = new ArrayList<>(512);

            for (Map.Entry<String, JsonElement> catEntry : root.entrySet()) {
                if (!catEntry.getValue().isJsonObject()) continue;
                JsonObject category = catEntry.getValue().getAsJsonObject();

                for (Map.Entry<String, JsonElement> itemEntry : category.entrySet()) {
                    String itemKey = itemEntry.getKey().toLowerCase();
                    if (!itemEntry.getValue().isJsonArray()) continue;
                    JsonArray orders = itemEntry.getValue().getAsJsonArray();

                    double buyPrice  = 0;
                    double sellPrice = 0;

                    for (JsonElement orderEl : orders) {
                        if (!orderEl.isJsonObject()) continue;
                        JsonObject order = orderEl.getAsJsonObject();
                        String side  = getStringOrNull(order, "orderSide");
                        double price = getDouble(order, "price");

                        if ("BUY".equalsIgnoreCase(side))       buyPrice  = price;
                        else if ("SELL".equalsIgnoreCase(side)) sellPrice = price;
                    }

                    if (buyPrice > 0 || sellPrice > 0) {
                        result.add(new MarketPrice(itemKey, buyPrice, sellPrice));
                    }
                }
            }

            VisotarisLogger.debug("Markt-API: {} Preiseinträge geladen.", result.size());
            return result;
        }
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