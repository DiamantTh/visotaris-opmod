package systems.diath.visotaris_opmod.commands;

import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.network.chat.Component;
import systems.diath.visotaris_opmod.model.PendingAction;
import systems.diath.visotaris_opmod.services.PendingConfirmationService;

/**
 * Registriert alle Client-Commands der Visotaris Mod.
 *
 * MC 26.x: {@code ClientCommandManager} wurde durch {@code ClientCommands} ersetzt
 * (fabric-command-api-v2 3.x), Minecraft.setScreen()/screen() sind nach
 * {@code Minecraft.gui} gewandert (Mojang-Refactoring der Gui-Klasse).
 *
 * Interne Bestätigungs-Commands:
 *   /.confirmRename  /.cancelRename
 *   /.confirmSign    /.cancelSign
 *
 * Bedienung erfolgt über ModMenu und Keybinds, nicht über öffentliche oder
 * serverseitige /visotaris-Commands. Nur die lokalen Bestätigungsrouten
 * bleiben für anklickbare Chat-Komponenten notwendig.
 */
public final class VisotarisCommands {

    private VisotarisCommands() {}

    public static void register(PendingConfirmationService confirmService) {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
            registerAll(dispatcher, confirmService)
        );
    }

    private static void registerAll(CommandDispatcher<FabricClientCommandSource> d,
                                    PendingConfirmationService confirmService) {

        // ── Bestätigungs-Commands ──────────────────────────────────────────────
        d.register(ClientCommands.literal(".confirmRename")
            .executes(ctx -> {
                boolean ok = confirmService.confirm(PendingAction.Type.RENAME);
                send(ctx.getSource(), ok ? "§aRename bestätigt." : "§cKein ausstehender Rename.");
                return ok ? 1 : 0;
            })
        );
        d.register(ClientCommands.literal(".cancelRename")
            .executes(ctx -> {
                confirmService.cancel(PendingAction.Type.RENAME);
                send(ctx.getSource(), "§7Rename abgebrochen.");
                return 1;
            })
        );
        d.register(ClientCommands.literal(".confirmSign")
            .executes(ctx -> {
                boolean ok = confirmService.confirm(PendingAction.Type.SIGN);
                send(ctx.getSource(), ok ? "§aSign bestätigt." : "§cKein ausstehender Sign.");
                return ok ? 1 : 0;
            })
        );
        d.register(ClientCommands.literal(".cancelSign")
            .executes(ctx -> {
                confirmService.cancel(PendingAction.Type.SIGN);
                send(ctx.getSource(), "§7Sign abgebrochen.");
                return 1;
            })
        );

    }

    @SuppressWarnings("null")
    private static void send(FabricClientCommandSource source, String text) {
        source.sendFeedback(Component.literal(text));
    }
}
