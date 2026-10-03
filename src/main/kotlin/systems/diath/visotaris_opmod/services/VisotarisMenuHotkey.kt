package systems.diath.visotaris_opmod.services

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.mojang.blaze3d.platform.InputConstants
import fi.dy.masa.malilib.config.ConfigManager
import fi.dy.masa.malilib.config.ConfigUtils
import fi.dy.masa.malilib.config.IConfigBase
import fi.dy.masa.malilib.config.IConfigHandler
import fi.dy.masa.malilib.config.gui.GuiModConfigs
import fi.dy.masa.malilib.config.options.ConfigHotkey
import fi.dy.masa.malilib.event.InputEventHandler
import fi.dy.masa.malilib.gui.GuiBase
import fi.dy.masa.malilib.gui.button.ButtonGeneric
import fi.dy.masa.malilib.hotkeys.IHotkey
import fi.dy.masa.malilib.hotkeys.IKeybindManager
import fi.dy.masa.malilib.hotkeys.IKeybindProvider
import fi.dy.masa.malilib.hotkeys.KeyAction
import fi.dy.masa.malilib.hotkeys.KeybindSettings
import fi.dy.masa.malilib.registry.Registry
import fi.dy.masa.malilib.util.KeyCodes
import fi.dy.masa.malilib.util.data.ModInfo
import net.minecraft.client.Minecraft
import net.fabricmc.loader.api.FabricLoader
import systems.diath.visotaris_opmod.VisotarisLogger
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

/** Registers the sole Visotaris menu shortcut with MaLiLib's shared multi-key system. */
class VisotarisMenuHotkey(private val openMenu: () -> Unit) {
    private val hotkey = ConfigHotkey(
        "openMenu",
        "LEFT_ALT,V",
        KeybindSettings.create(KeybindSettings.Context.INGAME, KeyAction.PRESS, false, false, true, true),
        "Open the Visotaris in-game menu",
        "Visotaris-Menü öffnen",
        "visotaris_opmod.hotkey.open_menu",
    )
    private val configPath = FabricLoader.getInstance().configDir.resolve("visotaris_opmod_hotkeys.json")
    private val configHandler = HotkeyConfigHandler(configPath, hotkey)
    private val provider = object : IKeybindProvider {
        override fun addKeysToMap(manager: IKeybindManager) {
            manager.addKeybindToMap(hotkey.keybind)
        }

        override fun addHotkeys(manager: IKeybindManager) {
            manager.addHotkeysForCategory(
                "Visotaris OPMod",
                "visotaris_opmod.hotkeys.category",
                listOf(hotkey),
            )
        }
    }

    init {
        instance = this
        Registry.CONFIG_SCREEN.registerConfigScreenFactory(
            ModInfo("visotaris_opmod", "Visotaris OPMod") { createHotkeyConfigScreen() },
        )
        val hadHotkeyConfig = Files.exists(configPath)
        ConfigManager.getInstance().registerConfigHandler("visotaris_opmod", configHandler)
        configHandler.load()

        // Keep a previously customised vanilla binding on first migration; an existing
        // MaLiLib file always wins, including an intentionally-cleared binding.
        if (!hadHotkeyConfig) migrateLegacyBinding()?.let { key ->
            hotkey.setValueFromString(key)
            configHandler.save()
        }

        hotkey.keybind.setCallback { _, _ ->
            openMenu()
            true
        }

        val keybindManager = InputEventHandler.getKeybindManager()
        keybindManager.registerKeybindProvider(provider)
        keybindManager.updateUsedKeys()
    }

    /** Uses MaLiLib's own keybind editor, scoped to Visotaris' menu shortcut only. */
    private fun showConfigScreen() {
        GuiBase.openGui(createHotkeyConfigScreen())
    }

    private fun createHotkeyConfigScreen(): HotkeyConfigScreen {
        val parent = Minecraft.getInstance().gui.screen()
        return HotkeyConfigScreen().apply {
            if (parent != null) setParent(parent)
        }
    }

    private inner class HotkeyConfigScreen : GuiModConfigs(
        "visotaris_opmod",
        listOf(hotkey),
        "visotaris_opmod.hotkeys.title",
    ) {
        override fun onSettingsChanged() {
            configHandler.save()
            InputEventHandler.getKeybindManager().updateUsedKeys()
        }

        override fun initGui() {
            super.initGui()
            val parent = getParent() ?: return
            val label = "visotaris_opmod.hotkeys.back"
            val buttonWidth = 168
            val button = ButtonGeneric(
                (getScreenWidth() - buttonWidth) / 2,
                getScreenHeight() - 30,
                buttonWidth,
                false,
                label,
            )
            addButton(button) { _, _ -> GuiBase.openGui(parent) }
        }
    }

    private fun migrateLegacyBinding(): String? {
        val optionsFile = FabricLoader.getInstance().gameDir.resolve("options.txt")
        if (!Files.isRegularFile(optionsFile)) return null

        return try {
            val prefix = "key_visotaris_opmod.key.open_settings:"
            val storedName = Files.readAllLines(optionsFile)
                .firstOrNull { it.startsWith(prefix) }
                ?.substringAfter(':')
                ?.trim()
                ?: return null
            if (storedName == "key.keyboard.unknown") return null

            val key = InputConstants.getKey(storedName)
            if (key.type != InputConstants.Type.KEYSYM || key.value < 0) return null
            KeyCodes.getNameForKey(key.value)
        } catch (exception: Exception) {
            VisotarisLogger.warn("Alte Visotaris-Menübelegung konnte nicht migriert werden: {}", exception.message)
            null
        }
    }

    private class HotkeyConfigHandler(private val path: Path, private val hotkey: ConfigHotkey) : IConfigHandler {
        private val options: List<IConfigBase> = listOf(hotkey)

        override fun load() {
            if (!Files.isRegularFile(path)) return
            try {
                val root = JsonParser.parseString(Files.readString(path)).asJsonObject
                ConfigUtils.readConfigBase(root, "visotaris_opmod", options)
            } catch (exception: Exception) {
                VisotarisLogger.warn("MaLiLib-Hotkeykonfiguration konnte nicht gelesen werden: {}", exception.message)
            }
        }

        override fun save() {
            try {
                Files.createDirectories(path.parent)
                val root = JsonObject()
                ConfigUtils.writeConfigBase(root, "visotaris_opmod", options)
                val temporary = path.resolveSibling(path.fileName.toString() + ".tmp")
                try {
                    Files.writeString(temporary, root.toString() + System.lineSeparator())
                    try {
                        Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
                    } catch (_: AtomicMoveNotSupportedException) {
                        Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING)
                    }
                } finally {
                    Files.deleteIfExists(temporary)
                }
            } catch (exception: Exception) {
                VisotarisLogger.warn("MaLiLib-Hotkeykonfiguration konnte nicht gespeichert werden: {}", exception.message)
            }
        }
    }

    companion object {
        private lateinit var instance: VisotarisMenuHotkey

        @JvmStatic
        fun openConfigScreen() {
            if (::instance.isInitialized) instance.showConfigScreen()
        }
    }
}
