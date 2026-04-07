package systems.diath.visotaris;

import net.fabricmc.loader.api.FabricLoader;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import systems.diath.visotaris.config.VisotarisConfig;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.concurrent.TimeUnit;

/**
 * Gemeinsame Konstanten (Mod-ID, Name) – verhindert Duplikate
 * zwischen den versionsgebundenen VisotarisModClient-Klassen und VisotarisLogger.
 */
public final class VisotarisConst {

    public static final String MOD_ID   = "visotaris";
    public static final String MOD_NAME = "Visotaris";

    /**
     * Baut den HTTP User-Agent-String.
     * Enthält Mod-, MC- und Fabric-Loader-Version dynamisch aus FabricLoader.
     *
     * @param custom Wenn nicht leer, wird dieser Wert direkt zurückgegeben.
     */
    public static String buildUserAgent(String custom) {
        if (custom != null && !custom.isBlank()) return custom.strip();

        FabricLoader loader = FabricLoader.getInstance();
        String modVer     = loader.getModContainer(MOD_ID)
            .map(c -> c.getMetadata().getVersion().getFriendlyString()).orElse("?");
        String mcVer      = loader.getModContainer("minecraft")
            .map(c -> c.getMetadata().getVersion().getFriendlyString()).orElse("?");
        String fabricVer  = loader.getModContainer("fabricloader")
            .map(c -> c.getMetadata().getVersion().getFriendlyString()).orElse("?");

        return "Visotaris/" + modVer
            + " (MC/" + mcVer
            + "; Fabric/" + fabricVer
            + "; git.diath.systems/DiamantTh/visotaris-opmod)";
    }

    /**
     * Erstellt einen {@link OkHttpClient} mit optionalem Proxy und Interceptor
     * für User-Agent, Accept-Header und Bearer-Authentifizierung.
     *
     * <p>Der Client wird einmalig pro Service-Instanz gebaut und wiederverwendet.
     * OkHttp nutzt HTTP/2 automatisch (via ALPN/TLS-Aushandlung).
     *
     * @param cfg Aktuelle Mod-Konfiguration
     */
    public static OkHttpClient buildOkHttpClient(VisotarisConfig cfg) {
        OkHttpClient.Builder builder = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .followRedirects(true)
            .addInterceptor(chain -> {
                Request.Builder req = chain.request().newBuilder()
                    .header("User-Agent", buildUserAgent(cfg.customUserAgent))
                    .header("Accept", "application/json");
                if (cfg.apiKey != null && !cfg.apiKey.isBlank()) {
                    req.header("Authorization", "Bearer " + cfg.apiKey.strip());
                }
                return chain.proceed(req.build());
            });
        if (cfg.proxyHost != null && !cfg.proxyHost.isBlank() && cfg.proxyPort > 0) {
            builder.proxy(new Proxy(Proxy.Type.HTTP,
                new InetSocketAddress(cfg.proxyHost.strip(), cfg.proxyPort)));
        }
        return builder.build();
    }

    private VisotarisConst() {}
}
