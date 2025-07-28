package org.samo_lego.antilogout.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.samo_lego.antilogout.AntiLogout;
import org.samo_lego.antilogout.datatracker.LogoutRules;

import java.util.Collections;

import static net.minecraft.server.command.CommandManager.literal;
import static org.samo_lego.antilogout.AntiLogout.config;

public class AfkCommand {

    /**
     * Utility method to check if a source has the required permission.
     *
     * @param source CommandSourceStack
     * @param permission Permission string
     * @param level Required permission level
     * @return true if allowed, false otherwise
     */
    private static boolean hasPermission(ServerCommandSource source, String permission, int level) {
        return Permissions.check(source, permission, level);
    }

    /**
     * Registers /afk command with improved feedback and help.
     * Usage: /afk := puts executor afk for max time.
     *        /afk players [targets] [time] := puts specified players afk for specified time.
     *        /afk time [time] := puts executor afk for specified time.
     *        /afk help := shows usage info.
     *
     * @param dispatcher command dispatcher
     */
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(literal("afk")
            .requires(src -> hasPermission(src, "antilogout.command.afk", config.afk.permissionLevel))
            .then(literal("help")
                .executes(ctx -> {
                    ctx.getSource().sendFeedback(() -> Text.literal("/afk - Set yourself AFK for max time.\n" +
                        "/afk time <seconds> - Set yourself AFK for a specific time.\n" +
                        "/afk players <targets> [time <seconds>] - Set other players AFK for max or specific time."), false);
                    return 1;
                })
            )
            .then(literal("players")
                .requires(src -> hasPermission(src, "antilogout.command.afk.players", 4))
                .then(CommandManager.argument("targets", EntityArgumentType.players())
                    .then(literal("time")
                        .requires(src -> hasPermission(src, "antilogout.command.afk.players.time", config.afk.permissionLevel))
                        .then(CommandManager.argument("time", DoubleArgumentType.doubleArg(-1, config.afk.maxAfkTime == -1 ? Double.MAX_VALUE : config.afk.maxAfkTime))
                            .executes(ctx -> afkPlayers(ctx.getSource(), EntityArgumentType.getPlayers(ctx, "targets"), DoubleArgumentType.getDouble(ctx, "time")))))
                    .executes(ctx -> afkPlayers(ctx.getSource(), EntityArgumentType.getPlayers(ctx, "targets"), config.afk.maxAfkTime))
                )
            )
            .then(literal("time")
                .requires(src -> hasPermission(src, "antilogout.command.afk.time", config.afk.permissionLevel))
                .then(CommandManager.argument("time", DoubleArgumentType.doubleArg(-1, config.afk.maxAfkTime == -1 ? Double.MAX_VALUE : config.afk.maxAfkTime))
                    .executes(ctx -> afkPlayers(ctx.getSource(), Collections.singletonList(ctx.getSource().getPlayerOrThrow()), DoubleArgumentType.getDouble(ctx, "time")))))
            .executes(ctx -> afkPlayers(ctx.getSource(), Collections.singleton(ctx.getSource().getPlayerOrThrow()), config.afk.maxAfkTime))
        );
    }

    // Simple cooldown map for self-AFK to prevent spamming
    private static final java.util.Map<java.util.UUID, Long> afkCooldowns = new java.util.HashMap<>();
    private static final long AFK_COOLDOWN_MS = 5000; // 5 seconds

    /**
     * Attempts to put the specified players in AFK mode.
     * Provides detailed feedback for each player.
     *
     * @param source  command source (for feedback)
     * @param players players to afk
     * @param timeLimit time in seconds
     * @return 1 if at least one player was set AFK, 0 otherwise
     */
    private static int afkPlayers(ServerCommandSource source, Iterable<ServerPlayerEntity> players, double timeLimit) {
        int affected = 0;
        boolean isSelf = false;
        for (var player : players) {
            LogoutRules rules = (LogoutRules) player;
            isSelf = source.isExecutedByPlayer() && source.getPlayer().equals(player);
            // Cooldown for self-AFK
            if (isSelf) {
                long now = System.currentTimeMillis();
                long last = afkCooldowns.getOrDefault(player.getUuid(), 0L);
                if (now - last < AFK_COOLDOWN_MS) {
                    source.sendError(Text.literal("You must wait before using /afk again."));
                    if (config.general.debug) AntiLogout.LOGGER.info("[AFK] {} tried to AFK but is on cooldown.", player.getName().getString());
                    continue;
                }
                afkCooldowns.put(player.getUuid(), now);
            }
            if (rules.al_isFake()) {
                source.sendError(Text.literal(player.getName().getString() + " is already AFK/disconnected."));
                if (config.general.debug) AntiLogout.LOGGER.info("[AFK] {} is already AFK/disconnected (by {}).", player.getName().getString(), source.getName());
                continue;
            }
            // Prevent AFK if player is in combat (not allowed to disconnect due to combat)
            if (!rules.al_allowDisconnect()) {
                source.sendError(Text.literal(config.afk.afkCombatMessage));
                AntiLogout.LOGGER.info("[AFK] BLOCKED: {} is in combat, NOT disconnecting!", player.getName().getString());
                if (config.general.debug) AntiLogout.LOGGER.info("[AFK] {} could NOT be set AFK by {} (combat state: BLOCKED)", player.getName().getString(), source.getName());
                continue; // SKIP disconnect and broadcast!
            }
            // Only runs if not in combat:
            if (config.general.debug) AntiLogout.LOGGER.info("[AFK] About to disconnect {} (combat state: ALLOWED)", player.getName().getString());
            if (timeLimit == -1) {
                rules.al_setAllowDisconnectAt(-1); // Unlimited AFK
            } else {
                rules.al_setAllowDisconnectAt(System.currentTimeMillis() + (long) (timeLimit * 1000));
            }
            player.networkHandler.disconnect(AntiLogout.AFK_MESSAGE);
            source.sendFeedback(() -> Text.literal("Set " + player.getName().getString() + " AFK for " + (timeLimit == -1 ? "unlimited" : (int) timeLimit) + " seconds."), false);
            if (config.general.debug) AntiLogout.LOGGER.info("[AFK] {} set {} AFK for {} seconds. (combat state: ALLOWED)", source.getName(), player.getName().getString(), (timeLimit == -1 ? "unlimited" : (int) timeLimit));
            player.getServer().getPlayerManager().broadcast(
                Text.literal(config.afk.afkBroadcastMessage.replace("{player}", player.getName().getString())), false);
            affected++;
        }
        if (affected == 0) {
            source.sendError(Text.literal("No players were set AFK."));
            return 0;
        }
        return 1;
    }
}
