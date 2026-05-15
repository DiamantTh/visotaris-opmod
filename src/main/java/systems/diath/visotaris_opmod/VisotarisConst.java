package systems.diath.visotaris_opmod;

import net.fabricmc.loader.api.FabricLoader;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import systems.diath.visotaris_opmod.config.VisotarisConfig;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.Locale;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.SSLSocketFactory;

/**
 * Gemeinsame Konstanten (Mod-ID, Name) – verhindert Duplikate
 * zwischen den versionsgebundenen VisotarisModClient-Klassen und VisotarisLogger.
 */
public final class VisotarisConst {

    public static final String MOD_ID   = "visotaris_opmod";
    public static final String MOD_NAME = "Visotaris OPMod";
    public static final String DISCORD_APPLICATION_ID = loadDiscordApplicationId();

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

        return "Visotaris-OPMod/" + modVer
            + " (MC/" + mcVer
            + "; Fabric/" + fabricVer
            + "; git.diath.systems/DiamantTh/visotaris-opmod)";
    }

    /**
     * Erstellt einen {@link OkHttpClient} mit optionalem Proxy und Interceptor
     * für User-Agent und Accept-Header.
     *
     * <p>Proxy-Typen:
     * <ul>
     *   <li>HTTP: Klartextverbindung zum HTTP-Proxy; HTTPS-Ziele via CONNECT.</li>
     *   <li>HTTPS: TLS-Verbindung zum Proxy selbst; HTTPS-Ziele anschließend via CONNECT.</li>
     *   <li>SOCKS: SOCKS-Proxy über {@link Proxy.Type#SOCKS}.</li>
     * </ul>
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
                return chain.proceed(req.build());
            });
        if (cfg.proxyHost != null && !cfg.proxyHost.isBlank() && cfg.proxyPort > 0) {
            String proxyType = normalizeProxyType(cfg.proxyType);
            builder.proxy(new Proxy(resolveJavaProxyType(proxyType),
                new InetSocketAddress(cfg.proxyHost.strip(), cfg.proxyPort)));
            if ("HTTPS".equals(proxyType)) {
                builder.socketFactory(SSLSocketFactory.getDefault());
            }
        }
        return builder.build();
    }

    private static Proxy.Type resolveJavaProxyType(String proxyType) {
        return "SOCKS".equals(proxyType) ? Proxy.Type.SOCKS : Proxy.Type.HTTP;
    }

    private static String normalizeProxyType(String configuredType) {
        if (configuredType == null) return "HTTP";
        String normalized = configuredType.strip().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "HTTPS", "SOCKS" -> normalized;
            default -> "HTTP";
        };
    }

    /**
     * Gibt ein {@link File}-Objekt für ein Unterverzeichnis des Mod-Disk-Caches zurück.
     * Pfad: {@code .minecraft/cache/visotaris_opmod/<subdir>}.
     * Das Verzeichnis wird nicht automatisch erstellt.
     */
    public static File getCacheDir(String subdir) {
        return FabricLoader.getInstance().getGameDir()
            .resolve("cache").resolve("visotaris_opmod").resolve(subdir).toFile();
    }

    private static String loadDiscordApplicationId() {
        try (InputStream in = VisotarisConst.class.getClassLoader()
                .getResourceAsStream("visotaris_opmod.properties")) {
            if (in == null) return "";
            Properties props = new Properties();
            props.load(in);
            return props.getProperty("discordApplicationId", "").strip();
        } catch (IOException e) {
            return "";
        }
    }

    private VisotarisConst() {}
}
