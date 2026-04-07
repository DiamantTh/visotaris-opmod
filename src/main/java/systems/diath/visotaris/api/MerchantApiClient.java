package systems.diath.visotaris.api;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import systems.diath.visotaris.VisotarisConst;
import systems.diath.visotaris.VisotarisLogger;
import systems.diath.visotaris.config.ConfigManager;
import systems.diath.visotaris.model.ShardRate;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * HTTP-Client für die OPSUCHT-Merchant-/Shard-API.
 *
 * Echtes API-Format (verifiziert):
 * <pre>
 * [
 *   {"source":"diamond_block",  "target":"opshards", "exchangeRate":9.46},
 *   {"source":"netherite_ingot","target":"opshards", "exchangeRate":54.79},
 *   {"source":"minecraft:paper[item_name='{...}',custom_model_data=626,...]",
 *    "target":"opshards", "exchangeRate":20.96},
 *   ...
 * ]
 * </pre>
 *
 * Schlüssel-Strategie:
 * <ul>
 *   <li>Einfache Sources (kein {@code [}): Namespace-Präfix entfernen, lowercase.
 *       z.B. {@code diamond_block}, {@code netherite_ingot}</li>
 *   <li>Komplexe Sources (mit {@code [}): Item-ID vor {@code [} + {@code #CMD} Suffix.
 *       z.B. {@code paper#626} für custom_model_data=626.
 *       Damit kann der TooltipValueService per CMD-Wert gezielt nachschlagen.</li>
 * </ul>
 */
public final class MerchantApiClient {

    private static final String  ENDPOINT    = "https://api.opsucht.net/merchant/rates";
    private static final Gson    GSON        = new Gson();
    /** Extrahiert den Wert von custom_model_data=NNN aus einem komplexen MC-Komponentenstring. */
    private static final Pattern CMD_PATTERN = Pattern.compile("custom_model_data=(\\d+)");

    private final ConfigManager configManager;
    private final OkHttpClient  httpClient;

    public MerchantApiClient(ConfigManager configManager) {
        this.configManager = configManager;
        this.httpClient    = VisotarisConst.buildOkHttpClient(configManager.getConfig());
    }

    public List<ShardRate> fetchRates() throws IOException {
        Request request = new Request.Builder().url(ENDPOINT).build();

        try (Response response = httpClient.newCall(request).execute()) {
            int status = response.code();
            if (status != 200) throw new IOException("Merchant-API Status " + status);

            ResponseBody body = response.body();
            if (body == null) throw new IOException("Merchant-API: leerer Response-Body");

            try (InputStream is = body.byteStream();
                 InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {

                JsonElement root = GSON.fromJson(reader, JsonElement.class);

                if (!root.isJsonArray()) {
                    VisotarisLogger.warn("Merchant-API: unerwartetes Root-Element (kein Array).");
                    return Collections.emptyList();
                }

                JsonArray array = root.getAsJsonArray();
                List<ShardRate> result = new ArrayList<>(array.size());

                for (JsonElement el : array) {
                    if (!el.isJsonObject()) continue;
                    JsonObject obj = el.getAsJsonObject();

                    String rawSource = getStringOrNull(obj, "source");
                    if (rawSource == null || rawSource.isBlank()) continue;

                    double rate = getDouble(obj, "exchangeRate");
                    String key  = normalizeSource(rawSource);
                    result.add(new ShardRate(key, rate));
                }

                VisotarisLogger.debug("Merchant-API: {} Shardkurs-Einträge geladen.", result.size());
                return result;
            }
        }
    }

    /**
     * Wandelt den rohen {@code source}-String in einen stabilen, vergleichbaren Schlüssel.
     *
     * <p>Einfach: {@code "diamond_block"} → {@code "diamond_block"}
     * <p>Einfach mit Namespace: {@code "minecraft:diamond_block"} → {@code "diamond_block"}
     * <p>Komplex: {@code "minecraft:paper[...,custom_model_data=626,...]"} → {@code "paper#626"}
     * <p>Komplex ohne CMD: {@code "minecraft:paper[...]"} → {@code "paper"}
     */
    static String normalizeSource(String raw) {
        String s = raw.trim();

        if (s.contains("[")) {
            // Komplexer MC-Komponentenstring: Basis-ID vor '[' extrahieren
            int bracket = s.indexOf('[');
            String base = s.substring(0, bracket);
            if (base.contains(":")) base = base.substring(base.lastIndexOf(':') + 1);
            base = base.toLowerCase();

            // custom_model_data-Wert als Suffix anhängen (für gezieltes Lookup)
            Matcher m = CMD_PATTERN.matcher(s);
            return m.find() ? base + "#" + m.group(1) : base;
        }

        // Einfacher Source-String
        if (s.contains(":")) s = s.substring(s.lastIndexOf(':') + 1);
        return s.toLowerCase().replace(" ", "_");
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