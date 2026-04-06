package systems.diath.noopmod;

import net.fabricmc.loader.api.FabricLoader;

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

    private NoOpConst() {}
}
