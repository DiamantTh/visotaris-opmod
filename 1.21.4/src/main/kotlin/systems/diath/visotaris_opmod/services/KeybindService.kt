package systems.diath.visotaris_opmod.services

import com.mojang.blaze3d.platform.InputConstants
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper
import net.minecraft.client.KeyMapping
import org.lwjgl.glfw.GLFW
import systems.diath.visotaris_opmod.config.ConfigManager
import systems.diath.visotaris_opmod.config.VisotarisConfigScreen

/**
 * MC 1.21.4 – official mappings use KeyMapping with a string category.
 */
class KeybindService(
    private val config: ConfigManager,
    private val marketSync: MarketSyncService,
    private val merchantSync: MerchantSyncService,
) {
    companion object {
        private const val CATEGORY = "key.categories.visotaris_opmod"
    }

    val keyOpenSettings: KeyMapping = KeyBindingHelper.registerKeyBinding(
        KeyMapping("visotaris_opmod.key.open_settings",
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_UNKNOWN, CATEGORY)
    )
    val keyToggleHud: KeyMapping = KeyBindingHelper.registerKeyBinding(
        KeyMapping("visotaris_opmod.key.toggle_hud",
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_UNKNOWN, CATEGORY)
    )
    val keyRefreshMarket: KeyMapping = KeyBindingHelper.registerKeyBinding(
        KeyMapping("visotaris_opmod.key.refresh_market",
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_UNKNOWN, CATEGORY)
    )

    fun registerTick() {
        ClientTickEvents.END_CLIENT_TICK.register { mc ->
            while (keyOpenSettings.consumeClick()) {
                mc.setScreen(VisotarisConfigScreen(mc.screen))
            }
            while (keyToggleHud.consumeClick()) {
                config.getConfig().showHud = !config.getConfig().showHud
                config.save()
            }
            while (keyRefreshMarket.consumeClick()) {
                marketSync.refresh()
                merchantSync.refresh()
            }
        }
    }
}
