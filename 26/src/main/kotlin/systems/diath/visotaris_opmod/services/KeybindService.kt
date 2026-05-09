package systems.diath.visotaris_opmod.services

import com.mojang.blaze3d.platform.InputConstants
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper
import net.minecraft.client.KeyMapping
import org.lwjgl.glfw.GLFW
import systems.diath.visotaris_opmod.config.ConfigManager
import systems.diath.visotaris_opmod.config.VisotarisConfigScreen

/**
 * MC 26.x – Mojang-Klassen: KeyMapping (statt KeyBinding), InputConstants (statt InputUtil).
 * Fabric API: fabric-key-mapping-api-v1 / KeyMappingHelper (statt KeyBindingHelper).
 */
class KeybindService(
    private val config: ConfigManager,
    private val marketSync: MarketSyncService,
    private val merchantSync: MerchantSyncService,
    private val discordScreenshots: DiscordScreenshotService,
) {
    companion object {
        private val CATEGORY: KeyMapping.Category = KeyMapping.Category.MISC
    }

    val keyOpenSettings: KeyMapping = KeyMappingHelper.registerKeyMapping(
        KeyMapping("visotaris_opmod.key.open_settings",
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_UNKNOWN, CATEGORY)
    )
    val keyToggleHud: KeyMapping = KeyMappingHelper.registerKeyMapping(
        KeyMapping("visotaris_opmod.key.toggle_hud",
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_UNKNOWN, CATEGORY)
    )
    val keyRefreshMarket: KeyMapping = KeyMappingHelper.registerKeyMapping(
        KeyMapping("visotaris_opmod.key.refresh_market",
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_UNKNOWN, CATEGORY)
    )
    private val screenshotKeys: List<KeyMapping> = (1..DiscordScreenshotService.TARGET_COUNT).map { index ->
        KeyMappingHelper.registerKeyMapping(
            KeyMapping("visotaris_opmod.key.discord_screenshot_$index",
                InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_UNKNOWN, CATEGORY)
        )
    }

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
            screenshotKeys.forEachIndexed { index, key ->
                while (key.consumeClick()) {
                    discordScreenshots.sendToTarget(index)
                }
            }
        }
    }
}
