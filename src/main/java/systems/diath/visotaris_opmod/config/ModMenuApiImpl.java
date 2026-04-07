package systems.diath.visotaris_opmod.config;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/**
 * Mod Menu Integration.
 * Registriert den Config-Screen in der Mod Menu Oberfläche.
 *
 * Wird im "modmenu"-Entrypoint in fabric.mod.json eingetragen.
 * Phase 3: ConfigScreen wird hier eingehängt.
 *
 * TODO: ConfigScreen implementieren und hier verknüpfen.
 */
@Environment(EnvType.CLIENT)
public final class ModMenuApiImpl implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> new VisotarisConfigScreen(parent);
    }
}
