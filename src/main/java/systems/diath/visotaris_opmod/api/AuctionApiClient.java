package systems.diath.visotaris_opmod.api;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import okhttp3.Cache;
import okhttp3.CacheControl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import systems.diath.visotaris_opmod.VisotarisConst;
import systems.diath.visotaris_opmod.config.ConfigManager;
import systems.diath.visotaris_opmod.model.Auction;
import systems.diath.visotaris_opmod.model.AuctionCategory;

import java.io.IOException;
import java.lang.reflect.Type;
import java.time.Instant;
import java.util.List;

/** HTTP transport for the public, read-only OPSUCHT auction endpoints. */
public final class AuctionApiClient {
    public static final String BASE_URL = "https://api.opsucht.net/auctions";
    private static final Gson GSON = new GsonBuilder()
        .registerTypeAdapter(Instant.class, (com.google.gson.JsonDeserializer<Instant>) (v, t, c) -> Instant.parse(v.getAsString()))
        .create();
    private final OkHttpClient http;

    public AuctionApiClient(ConfigManager config) {
        http = VisotarisConst.buildOkHttpClient(config.getConfig()).newBuilder()
            .cache(new Cache(VisotarisConst.getCacheDir("auctions"), 3L * 1024 * 1024)).build();
    }
    public List<AuctionCategory> fetchCategories() throws IOException { return fetch("/categories", new com.google.gson.reflect.TypeToken<List<AuctionCategory>>(){}.getType()); }
    public List<Auction> fetchActive() throws IOException { return fetch("/active", new com.google.gson.reflect.TypeToken<List<Auction>>(){}.getType()); }
    public Response openStream(String lastEventId) throws IOException {
        Request.Builder builder = new Request.Builder().url(BASE_URL + "/stream").header("Accept", "text/event-stream").cacheControl(CacheControl.FORCE_NETWORK);
        if (lastEventId != null && !lastEventId.isBlank()) builder.header("Last-Event-ID", lastEventId);
        Response response = http.newCall(builder.build()).execute();
        if (!response.isSuccessful() || response.body() == null) { response.close(); throw new IOException("Auction stream status " + response.code()); }
        return response;
    }
    private <T> T fetch(String path, Type type) throws IOException {
        Request request = new Request.Builder().url(BASE_URL + path).cacheControl(CacheControl.FORCE_NETWORK).build();
        try (Response response = http.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) throw new IOException("Auction API status " + response.code());
            return GSON.fromJson(response.body().charStream(), type);
        }
    }
    public static Auction parseAuction(JsonElement element) {
        if (element != null && element.isJsonObject() && element.getAsJsonObject().has("auction")) {
            element = element.getAsJsonObject().get("auction");
        }
        return GSON.fromJson(element, Auction.class);
    }
    public static JsonElement parseElement(String json) { return com.google.gson.JsonParser.parseString(json); }
}
