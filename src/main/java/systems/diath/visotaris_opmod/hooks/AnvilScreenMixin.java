package systems.diath.visotaris_opmod.hooks;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AnvilScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import systems.diath.visotaris_opmod.VisotarisModClient;
import systems.diath.visotaris_opmod.util.AmountShortformExpander;

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
    private EditBox name;

    /**
     * Rekursionsschutz: verhindert Endlosschleife,
     * wenn setValue() wieder onRenamed() auslöst.
     */
    private boolean visotaris$expanding = false;

    // Passt auf: 1k  1.5k  1,5k  2m  500k  1b  usw. als gesamten Feldinhalt.
    // Optionale führende/nachfolgende Leerzeichen werden toleriert.
    private static final Pattern SHORTHAND_FULL = Pattern.compile(
        "^\\s*(\\d+(?:[.,]\\d+)?)\\s*([kKmMbB])\\s*$"
    );

    /**
     * Injiziert vor der originalen AnvilScreen.onNameChanged-Logik.
     * Prüft, ob der gesamte Textfeld-Inhalt eine Kurzform ist –
     * wenn ja, setzt es die voll ausgeschriebene Zahl.
     */
    @Inject(method = "onNameChanged", at = @At("HEAD"), cancellable = true)
    private void visotaris$onNameChanged(String text, CallbackInfo ci) {
        if (visotaris$expanding) return;

        VisotarisModClient mod = VisotarisModClient.getInstance();
        if (mod == null) return;
        var cfg = mod.getConfigManager().getConfig();
        if (!cfg.ingameFeaturesEnabled()) return;
        if (!cfg.enableAnvilNormalization) return;

        Matcher m = SHORTHAND_FULL.matcher(text);
        if (!m.matches()) return;

        String expanded = AmountShortformExpander.expandAmount(m.group(1), m.group(2));
        if (expanded == null || expanded.equals(text.trim())) return;

        visotaris$expanding = true;
        try {
            // setValue() triggert erneut onRenamed – diesmal ohne Expansion
            this.name.setValue(expanded);
        } finally {
            visotaris$expanding = false;
        }

        // Originalen Aufruf mit dem unerweiterten Component abbrechen –
        // der setValue()-Aufruf hat bereits den korrekten Wert propagiert.
        ci.cancel();
    }
}
