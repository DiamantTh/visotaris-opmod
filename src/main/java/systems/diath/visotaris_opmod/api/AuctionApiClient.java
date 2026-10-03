package systems.diath.visotaris_opmod.api;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;
import com.google.gson.JsonDeserializer;
import okhttp3.Cache;
import okhttp3.CacheControl;
import okhttp3.Call;
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
import java.util.concurrent.TimeUnit;

/** HTTP transport for the public, read-only OPSUCHT auction endpoints. */
public final class AuctionApiClient {
    public static final String BASE_URL = "https://api.opsucht.net/auctions";
    private static final Gson GSON = new GsonBuilder()
        .registerTypeAdapter(Instant.class, new InstantAdapter())
        .create();
    private final OkHttpClient http;
    private final OkHttpClient streamHttp;

    public AuctionApiClient(ConfigManager config) {
        http = VisotarisConst.buildOkHttpClient(config.getConfig()).newBuilder()
            .cache(new Cache(VisotarisConst.getCacheDir("auctions"), 3L * 1024 * 1024)).build();
        streamHttp = http.newBuilder().readTimeout(0, TimeUnit.MILLISECONDS).build();
    }
    public List<AuctionCategory> fetchCategories() throws IOException { return fetch("/categories", new com.google.gson.reflect.TypeToken<List<AuctionCategory>>(){}.getType()); }
    public List<Auction> fetchActive() throws IOException { return fetch("/active", new com.google.gson.reflect.TypeToken<List<Auction>>(){}.getType()); }
    public Response openStream(String lastEventId) throws IOException {
        return newStreamCall(lastEventId).execute();
    }
    public Call newStreamCall(String lastEventId) {
        Request.Builder builder = new Request.Builder().url(BASE_URL + "/stream").header("Accept", "text/event-stream").cacheControl(CacheControl.FORCE_NETWORK);
        if (lastEventId != null && !lastEventId.isBlank()) builder.header("Last-Event-ID", lastEventId);
        return streamHttp.newCall(builder.build());
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
    public static JsonElement toJsonElement(Auction auction) { return GSON.toJsonTree(auction); }
    public static JsonElement parseElement(String json) { return com.google.gson.JsonParser.parseString(json); }

    private static final class InstantAdapter implements JsonSerializer<Instant>, JsonDeserializer<Instant> {
        @Override public JsonElement serialize(Instant value, Type type, com.google.gson.JsonSerializationContext context) {
            return value == null ? null : new JsonPrimitive(value.toString());
        }
        @Override public Instant deserialize(JsonElement value, Type type, com.google.gson.JsonDeserializationContext context) {
            return value == null || value.isJsonNull() ? null : Instant.parse(value.getAsString());
        }
    }
}
