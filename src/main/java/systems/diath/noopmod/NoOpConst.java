package systems.diath.noopmod;

import net.fabricmc.loader.api.FabricLoader;
import systems.diath.noopmod.config.NoOpConfig;

import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.http.HttpClient;
import java.time.Duration;

/**
 * Gemeinsame Konstanten (Mod-ID, Name) – verhindert Duplikate
 * zwischen den versionsgebundenen NoOpModClient-Klassen und NoOpLogger.
 */
public final class NoOpConst {

    public static final String MOD_ID   = "noopmod";
    public static final String MOD_NAME = "Visotaris OPMod";

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
     * Erstellt einen {@link HttpClient} mit HTTP/2-Präferenz und optionalem Proxy.
     *
     * @param cfg Aktuelle Mod-Konfiguration (proxyHost/proxyPort werden ausgewertet)
     */
    public static HttpClient buildHttpClient(NoOpConfig cfg) {
        HttpClient.Builder builder = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_2)
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL);
        if (cfg.proxyHost != null && !cfg.proxyHost.isBlank() && cfg.proxyPort > 0) {
            builder.proxy(ProxySelector.of(new InetSocketAddress(cfg.proxyHost.strip(), cfg.proxyPort)));
        }
        return builder.build();
    }

    private NoOpConst() {}
}
