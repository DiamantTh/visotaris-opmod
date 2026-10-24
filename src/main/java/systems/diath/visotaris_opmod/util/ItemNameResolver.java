package systems.diath.visotaris_opmod.util;

import net.minecraft.locale.Language;

import java.util.Locale;

/**
 * Übersetzt englische API-Item-IDs (z.B. {@code "acacia_leaves"}) in den
 * deutschen Anzeigenamen aus den Minecraft-Sprachressourcen.
 *
 * <p>Funktionsweise:
 * <ol>
 *   <li>API-Item-ID → Minecraft-Registry-ID ({@code minecraft:acacia_leaves})</li>
 *   <li>Item-/Block-Übersetzungs-Key anhand der Registry-ID bestimmen</li>
 *   <li>deutsche Minecraft-Sprachressource → deutscher Anzeigename</li>
 *   <li>{@link Language#getInstance()} → defensiver Fallback der aktiven Spielsprache</li>
 * </ol>
 *
 * <p>Unterstützt auch CMD-Compound-Keys wie {@code "paper#626"}: in diesem Fall
 * wird nur der Basis-Teil ({@code "paper"}) zur Lokalisierung verwendet.
 *
 * <p>Thread-safe: Nur lesender Zugriff auf Registries und Language-Singleton.
 * Darf auf dem Render-Thread aufgerufen werden.
 */
public final class ItemNameResolver {

    private ItemNameResolver() {}

    /**
     * Gibt den deutschen Item-Namen für die gegebene API-Item-ID zurück. Die aktive
     * Client-Sprache ist nur der Fallback, falls die deutsche Ressource unerwartet
     * nicht verfügbar ist.
     *
     * @param apiItemId  API-Item-ID in Kleinbuchstaben, z.B. {@code "acacia_leaves"}
     *                   oder CMD-Compound {@code "paper#626"}
     * @return  Lokalisierter Name (z.B. "Akazienblätter" auf Deutsch), oder
     *          die rohe {@code apiItemId} wenn kein Registry-Eintrag gefunden wird
     */
    public static String resolve(String apiItemId) {
        return resolve(apiItemId, AuctionDisplayText::germanTranslation,
            key -> Language.getInstance().getOrDefault(key, null));
    }

    static String resolve(String apiItemId, java.util.function.Function<String, String> germanTranslations,
                          java.util.function.Function<String, String> activeTranslations) {
        if (apiItemId == null || apiItemId.isBlank()) return apiItemId;

        // CMD-Compound-Key: "paper#626" → nur "paper" zur Lokalisierung, Suffix anhängen
        String suffix = "";
        String baseKey = apiItemId.trim();
        int hashIdx = apiItemId.indexOf('#');
        if (hashIdx >= 0) {
            baseKey = apiItemId.substring(0, hashIdx);
            suffix = " (" + apiItemId.substring(hashIdx + 1) + ")";
        }
        baseKey = baseKey.toLowerCase(Locale.ROOT);

        String namespace = "minecraft";
        String path = baseKey;
        int namespaceSeparator = baseKey.indexOf(':');
        if (namespaceSeparator >= 0) {
            namespace = baseKey.substring(0, namespaceSeparator);
            path = baseKey.substring(namespaceSeparator + 1);
        }

        // Check item and block keys directly so a market list never needs to scan the
        // registry per row. Prefer bundled German resources: auction snapshots often
        // omit displayName for ordinary vanilla items such as spawn eggs.
        String itemKey = "item." + namespace + "." + path;
        String blockKey = "block." + namespace + "." + path;
        String localizedName = translation(germanTranslations, itemKey);
        if (localizedName == null) localizedName = translation(germanTranslations, blockKey);
        if (localizedName == null) localizedName = translation(activeTranslations, itemKey);
        if (localizedName == null) localizedName = translation(activeTranslations, blockKey);

        if (localizedName == null) return apiItemId;
        return localizedName + suffix;
    }

    private static String translation(java.util.function.Function<String, String> translations, String key) {
        String value = translations.apply(key);
        return value == null || value.isBlank() || value.equals(key) ? null : value;
    }

    /**
     * Gibt den lokalisierten Namen zurück, oder {@code fallback} wenn die
     * Auflösung keinen bekannten Eintrag liefert.
     *
     * @param apiItemId API-Item-ID (wie in {@link #resolve(String)})
     * @param fallback  Rückgabewert bei unbekanntem Item
     */
    public static String resolveOrFallback(String apiItemId, String fallback) {
        String result = resolve(apiItemId);
        return result.equals(apiItemId) ? fallback : result;
    }

    /**
     * Resolves a known vanilla material name with the German Minecraft resource.
     * Unlike {@link #resolve(String)}, this returns {@code null} when the key
     * is not represented by Minecraft's bundled language resources.
     */
    public static String resolveVanilla(String material) {
        if (material == null || material.isBlank()) return null;

        String normalized = material.trim().toLowerCase(Locale.ROOT);
        String localized = resolve(normalized);
        return localized.equals(normalized) ? null : localized;
    }
}
