package systems.diath.visotaris_opmod.api;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import okhttp3.CacheControl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import systems.diath.visotaris_opmod.VisotarisConst;
import systems.diath.visotaris_opmod.VisotarisLogger;
import systems.diath.visotaris_opmod.config.ConfigManager;
import systems.diath.visotaris_opmod.model.PriceHistoryPoint;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * HTTP-Client für die OPSUCHT-Preisverlauf-API.
 *
 * Endpunkt: {@code GET https://api.opsucht.net/market/history/{material}}
 * Antwort: JSON-Array mit {@code timestamp}, {@code buyPrice}, {@code sellPrice}.
 */
public final class MarketHistoryApiClient {

    private static final String BASE_URL = "https://api.opsucht.net/market/history/";
    private static final Gson   GSON     = new Gson();

    private final OkHttpClient httpClient;

    public MarketHistoryApiClient(ConfigManager configManager) {
        this.httpClient = VisotarisConst.buildOkHttpClient(configManager.getConfig())
            .newBuilder()
            .build();
    }

    /**
     * Lädt den Preisverlauf für ein Material von der API.
     *
     * @param materialKey Item-Key in lowercase (z.B. {@code "diamond"})
     * @return Liste der Preisverlauf-Datenpunkte, leer bei Fehler
     * @throws IOException bei Netzwerkfehler ohne Cache-Fallback
     */
    public List<PriceHistoryPoint> fetchHistory(String materialKey) throws IOException {
        Request liveReq = new Request.Builder()
            .url(BASE_URL + materialKey)
            .cacheControl(CacheControl.FORCE_NETWORK)
            .build();
        try (Response response = httpClient.newCall(liveReq).execute()) {
            int status = response.code();
            if (status != 200) throw new IOException("History-API Status " + status + " für " + materialKey);
            ResponseBody body = response.body();
            if (body == null) throw new IOException("History-API: leerer Response-Body");
            return parseBodyStream(body, materialKey);
        } catch (IOException networkEx) {
            VisotarisLogger.warn("History-API offline für '{}': {}", materialKey, networkEx.getMessage());
            Request cacheReq = liveReq.newBuilder()
                .cacheControl(CacheControl.FORCE_CACHE)
                .build();
            try (Response cached = httpClient.newCall(cacheReq).execute()) {
                if (cached.code() == 504) throw networkEx;
                ResponseBody body = cached.body();
                if (body == null) throw networkEx;
                return parseBodyStream(body, materialKey);
            }
        }
    }

    private List<PriceHistoryPoint> parseBodyStream(ResponseBody body, String materialKey) throws IOException {
        try (InputStream is = body.byteStream();
             InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
            JsonElement root = GSON.fromJson(reader, JsonElement.class);
            if (!root.isJsonArray()) {
                VisotarisLogger.warn("History-API: unerwartetes Format für '{}'.", materialKey);
                return Collections.emptyList();
            }
            JsonArray array = root.getAsJsonArray();
            List<PriceHistoryPoint> result = new ArrayList<>(array.size());
            for (JsonElement el : array) {
                if (!el.isJsonObject()) continue;
                JsonObject obj = el.getAsJsonObject();
                long ts         = getLong(obj,   "timestamp");
                double buyPrice  = getDouble(obj, "buyPrice");
                double sellPrice = getDouble(obj, "sellPrice");
                result.add(new PriceHistoryPoint(ts, buyPrice, sellPrice));
            }
            VisotarisLogger.debug("History-API: {} Punkte für '{}' geladen.", result.size(), materialKey);
            return result;
        }
    }

    private static long getLong(JsonObject obj, String key) {
        JsonElement el = obj.get(key);
        return (el != null && el.isJsonPrimitive()) ? el.getAsLong() : 0L;
    }

    private static double getDouble(JsonObject obj, String key) {
        JsonElement el = obj.get(key);
        return (el != null && el.isJsonPrimitive()) ? el.getAsDouble() : 0.0;
    }
}
