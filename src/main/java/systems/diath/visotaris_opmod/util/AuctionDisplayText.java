package systems.diath.visotaris_opmod.util;

import net.minecraft.client.Minecraft;
import net.minecraft.locale.Language;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

/** German, user-facing text for auction metadata that arrives as API keys. */
public final class AuctionDisplayText {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm", Locale.GERMAN);
    private static final Map<String, String> KNOWN_GERMAN_ENCHANTMENTS = Map.ofEntries(
        Map.entry("aqua_affinity", "Wasseraffinität"),
        Map.entry("bane_of_arthropods", "Nemesis der Gliederfüßer"),
        Map.entry("binding_curse", "Fluch der Bindung"),
        Map.entry("blast_protection", "Explosionsschutz"),
        Map.entry("breach", "Durchbruch"),
        Map.entry("channeling", "Entladung"),
        Map.entry("density", "Dichte"),
        Map.entry("depth_strider", "Wasserläufer"),
        Map.entry("efficiency", "Effizienz"),
        Map.entry("feather_falling", "Federfall"),
        Map.entry("fire_aspect", "Verbrennung"),
        Map.entry("fire_protection", "Feuerschutz"),
        Map.entry("flame", "Flamme"),
        Map.entry("fortune", "Glück"),
        Map.entry("frost_walker", "Eisläufer"),
        Map.entry("impaling", "Harpune"),
        Map.entry("infinity", "Unendlichkeit"),
        Map.entry("knockback", "Rückstoß"),
        Map.entry("looting", "Plünderung"),
        Map.entry("loyalty", "Treue"),
        Map.entry("luck_of_the_sea", "Glück des Meeres"),
        Map.entry("lure", "Köder"),
        Map.entry("mending", "Reparatur"),
        Map.entry("multishot", "Mehrfachschuss"),
        Map.entry("piercing", "Durchschuss"),
        Map.entry("power", "Stärke"),
        Map.entry("projectile_protection", "Schussschutz"),
        Map.entry("protection", "Schutz"),
        Map.entry("punch", "Schlag"),
        Map.entry("quick_charge", "Schnellladen"),
        Map.entry("respiration", "Atmung"),
        Map.entry("riptide", "Sog"),
        Map.entry("sharpness", "Schärfe"),
        Map.entry("silk_touch", "Behutsamkeit"),
        Map.entry("smite", "Bann"),
        Map.entry("soul_speed", "Seelenläufer"),
        Map.entry("sweeping", "Schwungkraft"),
        Map.entry("sweeping_edge", "Schwungkraft"),
        Map.entry("swift_sneak", "Huschen"),
        Map.entry("thorns", "Dornen"),
        Map.entry("unbreaking", "Haltbarkeit"),
        Map.entry("vanishing_curse", "Fluch des Verschwindens"),
        Map.entry("wind_burst", "Windstoß"),
        Map.entry("lunge", "Ausfallschritt")
    );

    private static final Object RESOURCE_LOCK = new Object();
    private static volatile ResourceManager germanResourceManager;
    private static volatile Map<String, String> germanTranslations = Map.of();

    private AuctionDisplayText() { }

    public static String enchantment(String key, int level) {
        return enchantment(key, level, AuctionDisplayText::germanTranslation);
    }

    static String enchantment(String key, int level, Function<String, String> translations) {
        String path = enchantmentPath(key);
        if (path == null) return "Unbekannte Verzauberung " + levelText(level, translations);

        String translationKey = "enchantment." + namespace(key) + "." + path;
        String name = usableTranslation(translations.apply(translationKey), translationKey);
        if (name == null && "minecraft".equals(namespace(key))) {
            name = KNOWN_GERMAN_ENCHANTMENTS.get(path);
        }
        if (name == null) name = "Unbekannte Verzauberung (" + readablePath(path) + ")";
        return name + " " + levelText(level, translations);
    }

    public static String category(String raw) {
        if (raw == null || raw.isBlank()) return "Unbekannt";
        String normalized = raw.trim().toLowerCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
        return switch (normalized) {
            case "custom_items", "custom_item" -> "Benutzerdefinierte Items";
            case "opshards", "op_shards" -> "OPShards";
            case "weapons", "weapon" -> "Waffen";
            case "armor", "armour" -> "Rüstung";
            case "tools", "tool" -> "Werkzeuge";
            case "misc", "other", "others" -> "Sonstiges";
            default -> humanize(normalized);
        };
    }

    public static String status(String raw) {
        if (raw == null || raw.isBlank()) return "Unbekannt";
        String normalized = raw.trim().toLowerCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
        return switch (normalized) {
            case "active", "open" -> "Aktiv";
            case "sold" -> "Verkauft";
            case "instant_bought", "bought" -> "Sofort gekauft";
            case "expired" -> "Abgelaufen";
            case "cancelled", "canceled" -> "Abgebrochen";
            case "removed" -> "Entfernt";
            case "pending" -> "Ausstehend";
            default -> humanize(normalized);
        };
    }

    public static String timestamp(Instant value) {
        return value == null ? "–" : DATE_FORMAT.format(value.atZone(ZoneId.systemDefault()));
    }

    public static String germanTranslation(String key) {
        Minecraft client = Minecraft.getInstance();
        if (client == null) return null;
        String activeLanguage = client.options.languageCode;
        if (activeLanguage != null && activeLanguage.toLowerCase(Locale.ROOT).startsWith("de")) {
            String active = usableTranslation(Language.getInstance().getOrDefault(key, null), key);
            if (active != null) return active;
        }

        String fromGermanResources = germanTranslations(client.getResourceManager()).get(key);
        if (fromGermanResources != null && !fromGermanResources.isBlank()) return fromGermanResources;

        if (key.startsWith("enchantment.level.")) {
            String suffix = key.substring("enchantment.level.".length());
            try { return romanLevel(Integer.parseInt(suffix)); }
            catch (NumberFormatException ignored) { return null; }
        }
        return null;
    }

    private static Map<String, String> germanTranslations(ResourceManager manager) {
        if (manager == null) return Map.of();
        if (manager == germanResourceManager) return germanTranslations;
        synchronized (RESOURCE_LOCK) {
            if (manager == germanResourceManager) return germanTranslations;
            Map<String, String> loaded = new HashMap<>();
            var resource = manager.getResource(Identifier.fromNamespaceAndPath("minecraft", "lang/de_de.json"));
            if (resource.isPresent()) {
                try (InputStream stream = resource.get().open()) {
                    Language.loadFromJson(stream, loaded::put);
                } catch (IOException | RuntimeException ignored) {
                    loaded.clear();
                }
            }
            germanTranslations = Map.copyOf(loaded);
            germanResourceManager = manager;
            return germanTranslations;
        }
    }

    private static String levelText(int level, Function<String, String> translations) {
        if (level >= 1 && level <= 10) {
            String key = "enchantment.level." + level;
            String translated = usableTranslation(translations.apply(key), key);
            if (translated != null) return translated;
            return romanLevel(level);
        }
        return Integer.toString(level);
    }

    private static String romanLevel(int level) {
        String[] roman = {"", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"};
        return level >= 1 && level < roman.length ? roman[level] : Integer.toString(level);
    }

    private static String enchantmentPath(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String key = raw.trim().toLowerCase(Locale.ROOT);
        int colon = key.indexOf(':');
        String path = colon < 0 ? key : key.substring(colon + 1);
        if (path.isBlank() || !path.matches("[a-z0-9_./-]+")) return null;
        return path;
    }

    private static String namespace(String raw) {
        if (raw == null) return "minecraft";
        int colon = raw.indexOf(':');
        return colon < 0 ? "minecraft" : raw.substring(0, colon).trim().toLowerCase(Locale.ROOT);
    }

    private static String usableTranslation(String value, String key) {
        return value == null || value.isBlank() || value.equals(key) ? null : value;
    }

    private static String readablePath(String path) {
        return path.replace('/', ' ').replace('_', ' ').replace('-', ' ');
    }

    private static String humanize(String value) {
        String[] parts = value.replace(':', ' ').replace('/', ' ').replace('_', ' ').split("\\s+");
        StringBuilder result = new StringBuilder();
        for (String part : parts) {
            if (part.isBlank()) continue;
            if (!result.isEmpty()) result.append(' ');
            result.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return result.isEmpty() ? "Unbekannt" : result.toString();
    }
}
