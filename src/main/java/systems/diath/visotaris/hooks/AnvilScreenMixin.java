package systems.diath.visotaris.hooks;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.screen.ingame.AnvilScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import systems.diath.visotaris.VisotarisModClient;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Amboss-Normalisierung: Expandiert Mengenkurzformen (1k, 2.5m, 500k …)
 * im Umbenennen-Textfeld des Amboss-Screens zu vollen Ganzzahlen.
 *
 * Anwendungsfall: OPSUCHT-Plugin-UIs, die den Amboss-Screen als Texteingabe
 * nutzen, z. B. Preiseingabe-Dialoge.
 *
 * Aktivierbar über VisotarisConfig.enableAnvilNormalization (Standard: true).
 */
@Environment(EnvType.CLIENT)
@Mixin(AnvilScreen.class)
public abstract class AnvilScreenMixin {

    /** Das Rename-Textfeld – ist direkt auf AnvilScreen gemappt. */
    @Shadow
    private TextFieldWidget nameField;

    /**
     * Rekursionsschutz: verhindert Endlosschleife,
     * wenn setText() wieder onRenamed() auslöst.
     */
    private boolean visotaris$expanding = false;

    // Passt auf: 1k  1.5k  1,5k  2m  500k  1b  usw. als gesamten Feldinhalt.
    // Optionale führende/nachfolgende Leerzeichen werden toleriert.
    private static final Pattern SHORTHAND_FULL = Pattern.compile(
        "^\\s*(\\d+(?:[.,]\\d+)?)\\s*([kKmMbB])\\s*$"
    );

    /**
     * Injiziert vor der originalen AnvilScreen.onRenamed-Logik.
     * Prüft, ob der gesamte Textfeld-Inhalt eine Kurzform ist –
     * wenn ja, setzt es die voll ausgeschriebene Zahl.
     */
    @Inject(method = "onRenamed", at = @At("HEAD"), cancellable = true)
    private void visotaris$onRenamed(String text, CallbackInfo ci) {
        if (visotaris$expanding) return;

        VisotarisModClient mod = VisotarisModClient.getInstance();
        if (mod == null) return;
        if (!mod.getConfigManager().getConfig().enableAnvilNormalization) return;

        Matcher m = SHORTHAND_FULL.matcher(text);
        if (!m.matches()) return;

        String expanded = expandAmount(m.group(1), m.group(2));
        if (expanded == null || expanded.equals(text.trim())) return;

        visotaris$expanding = true;
        try {
            // setText() triggert erneut onRenamed – diesmal ohne Expansion
            this.nameField.setText(expanded);
        } finally {
            visotaris$expanding = false;
        }

        // Originalen Aufruf mit dem unerweiterten Text abbrechen –
        // der setText()-Aufruf hat bereits den korrekten Wert propagiert.
        ci.cancel();
    }

    // ── Private Helpers ───────────────────────────────────────────────────────

    static String expandAmount(String digits, String suffix) {
        // Punkt ist Tausendertrenner nur wenn auch ein Komma vorhanden (dt. Format).
        // Kein Komma → Punkt ist Dezimalzeichen (int'l Format, z.B. "1.5k").
        String normalized = digits.contains(",")
                ? digits.replace(".", "").replace(",", ".")
                : digits;
        double number;
        try {
            number = Double.parseDouble(normalized);
        } catch (NumberFormatException e) {
            return null;
        }
        double multiplier = switch (suffix.toLowerCase()) {
            case "k" -> 1_000.0;
            case "m" -> 1_000_000.0;
            case "b" -> 1_000_000_000.0;
            default  -> 0.0;
        };
        if (multiplier == 0.0) return null;
        return Long.toString((long)(number * multiplier));
    }
}
