package systems.diath.visotaris_opmod.commands;

import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.network.chat.Component;
import systems.diath.visotaris_opmod.model.PendingAction;
import systems.diath.visotaris_opmod.services.PendingConfirmationService;

/**
 * Registriert alle Client-Commands der Visotaris Mod.
 *
 * Interne Bestätigungs-Commands:
 *   /.confirmRename  /.cancelRename
 *   /.confirmSign    /.cancelSign
 *
 * Es werden bewusst keine Bediencommands wie {@code /visotaris refresh}
 * registriert. Einstellungen, Status und API-Refresh werden ausschließlich
 * über ModMenu/Keybinds ausgeführt und können daher nie beim Server landen.
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
        d.register(ClientCommandManager.literal(".confirmRename")
            .executes(ctx -> {
                boolean ok = confirmService.confirm(PendingAction.Type.RENAME);
                send(ctx.getSource(), ok ? "§aRename bestätigt." : "§cKein ausstehender Rename.");
                return ok ? 1 : 0;
            })
        );
        d.register(ClientCommandManager.literal(".cancelRename")
            .executes(ctx -> {
                confirmService.cancel(PendingAction.Type.RENAME);
                send(ctx.getSource(), "§7Rename abgebrochen.");
                return 1;
            })
        );
        d.register(ClientCommandManager.literal(".confirmSign")
            .executes(ctx -> {
                boolean ok = confirmService.confirm(PendingAction.Type.SIGN);
                send(ctx.getSource(), ok ? "§aSign bestätigt." : "§cKein ausstehender Sign.");
                return ok ? 1 : 0;
            })
        );
        d.register(ClientCommandManager.literal(".cancelSign")
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
