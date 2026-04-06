package systems.diath.noopmod.commands;

import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import systems.diath.noopmod.NoOpModClient;
import systems.diath.noopmod.model.PendingAction;
import systems.diath.noopmod.services.PendingConfirmationService;

/**
 * Registriert alle Client-Commands der Visotaris OPMod.
 *
 * Interne Bestätigungs-Commands:
 *   /.confirmRename  /.cancelRename
 *   /.confirmSign    /.cancelSign
 *
 * Sonstige Commands:
 *   /noopmod refresh   – Markt & Shard-Daten sofort neu laden
 *   /noopmod tracker   – aktuellen Job-Snapshot ausgeben
 *   /noopmod status    – Mod-Status anzeigen
 *   /noopmod settings  – Config-Screen öffnen
 */
public final class NoOpCommands {

    private NoOpCommands() {}

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

        // ── Verwaltungs-Commands ───────────────────────────────────────────────
        d.register(ClientCommandManager.literal("noopmod")
            .then(ClientCommandManager.literal("refresh")
                .executes(ctx -> {
                    NoOpModClient mod = NoOpModClient.getInstance();
                    mod.getMarketSyncService().refresh();
                    mod.getMerchantSyncService().refresh();
                    send(ctx.getSource(), "§aMarkt & Shard-Daten werden neu geladen...");
                    return 1;
                })
            )
            .then(ClientCommandManager.literal("tracker")
                .executes(ctx -> {
                    var snap = NoOpModClient.getInstance().getJobTrackerService().getSnapshot();
                    send(ctx.getSource(), "§eJob: §f" + snap.getJobName()
                        + " §eLevel: §f" + snap.getLevel()
                        + " §eXP/h: §f" + String.format("%.0f", snap.getXpPerHour())
                        + " §e$/h: §f" + String.format("%.0f", snap.getMoneyPerHour())
                    );
                    return 1;
                })
            )
            .then(ClientCommandManager.literal("status")
                .executes(ctx -> {
                    NoOpModClient mod = NoOpModClient.getInstance();
                    send(ctx.getSource(), "§b[Visotaris OPMod] Status");
                    send(ctx.getSource(), "  Marktpreise: §f" +
                        (mod.getMarketCache().isEmpty() ? "§cnicht geladen" : "§ageladen"));
                    send(ctx.getSource(), "  Shardkurse:  §f" +
                        (mod.getShardCache().isEmpty() ? "§cnicht geladen" : "§ageladen"));
                    return 1;
                })
            )
            .then(ClientCommandManager.literal("settings")
                .executes(ctx -> {
                    MinecraftClient mc = MinecraftClient.getInstance();
                    mc.setScreen(new systems.diath.noopmod.config.NoOpConfigScreen(mc.currentScreen));
                    return 1;
                })
            )
        );
    }

    private static void send(FabricClientCommandSource source, String text) {
        source.sendFeedback(Text.literal(text));
    }
}
