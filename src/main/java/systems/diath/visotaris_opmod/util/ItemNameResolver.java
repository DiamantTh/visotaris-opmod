package systems.diath.visotaris_opmod.util;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.locale.Language;

/**
 * Übersetzt englische API-Item-IDs (z.B. {@code "acacia_leaves"}) in den
 * lokalisierten Anzeigenamen der aktuellen Minecraft-Spielsprache.
 *
 * <p>Funktionsweise:
 * <ol>
 *   <li>API-Item-ID → Minecraft-Registry-ID ({@code minecraft:acacia_leaves})</li>
 *   <li>Registry-Lookup → {@link Item}</li>
 *   <li>{@link Item#getDescriptionId()} → Übersetzungs-Key ({@code block.minecraft.acacia_leaves})</li>
 *   <li>{@link Language#getInstance()} → lokalisierter String in der aktiven Spielsprache</li>
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
     * Gibt den lokalisierten Item-Namen für die gegebene API-Item-ID zurück.
     *
     * @param apiItemId  API-Item-ID in Kleinbuchstaben, z.B. {@code "acacia_leaves"}
     *                   oder CMD-Compound {@code "paper#626"}
     * @return  Lokalisierter Name (z.B. "Akazienblätter" auf Deutsch), oder
     *          die rohe {@code apiItemId} wenn kein Registry-Eintrag gefunden wird
     */
    public static String resolve(String apiItemId) {
        if (apiItemId == null || apiItemId.isBlank()) return apiItemId;

        // CMD-Compound-Key: "paper#626" → nur "paper" zur Lokalisierung, Suffix anhängen
        String suffix = "";
        String baseKey = apiItemId;
        int hashIdx = apiItemId.indexOf('#');
        if (hashIdx >= 0) {
            baseKey = apiItemId.substring(0, hashIdx);
            suffix  = " (" + apiItemId.substring(hashIdx + 1) + ")";
        }

        // minecraft-Namespace versuchen; bei Vanilla-Items immer korrekt
        final String lookupKey = baseKey;
        Item item = BuiltInRegistries.ITEM.stream()
            .filter(candidate -> {
                var id = BuiltInRegistries.ITEM.getKey(candidate);
                return id != null && "minecraft".equals(id.getNamespace()) && lookupKey.equals(id.getPath());
            })
            .findFirst()
            .orElse(Items.AIR);

        // Items.AIR ist der Fallback wenn nichts gefunden – in dem Fall den Key zurückgeben
        if (item == Items.AIR && !"air".equals(baseKey)) {
            return apiItemId;   // unbekanntes Item → Roh-ID beibehalten
        }

        Language lang = Language.getInstance();
        String translationKey = item.getDescriptionId();
        String localizedName  = lang.getOrDefault(translationKey, null);

        // Wenn Language den Key nicht kennt, roh zurückgeben
        if (localizedName == null || localizedName.equals(translationKey)) {
            return apiItemId + suffix;
        }
        return localizedName + suffix;
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
}
