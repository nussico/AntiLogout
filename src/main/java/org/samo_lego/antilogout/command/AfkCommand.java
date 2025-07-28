package org.samo_lego.antilogout.command;

import java.util.Collections;
import java.util.Objects;

import org.samo_lego.antilogout.AntiLogout;
import static org.samo_lego.antilogout.AntiLogout.config;
import org.samo_lego.antilogout.datatracker.LogoutRules;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;

import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import static net.minecraft.server.command.CommandManager.literal;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public class AfkCommand {

    /**
     * Checks if a command source has the required permission and level.
     *
     * @param source     the command source
     * @param permission the permission string
     * @param level      the required permission level
     * @return true if allowed, false otherwise
     */
    private static boolean hasPermission(ServerCommandSource source, String permission, int level) {
        return Permissions.check(source, permission, level);
    }

    /**
     * Registers the /afk command and all its subcommands.
     *
     * Usage:
     *   /afk - Set yourself AFK for max time.
     *   /afk time <seconds> - Set yourself AFK for a specific time.
     *   /afk players <targets> [time <seconds>] - Set other players AFK for max or specific time.
     *   /afk help - Show usage info.
     *
     * @param dispatcher the command dispatcher
     */
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(literal("afk")
            .requires(src -> hasPermission(src, "antilogout.command.afk", config.afk.permissionLevel))
            .then(literal("help")
                .executes(ctx -> {
                    ctx.getSource().sendFeedback(() -> Text.literal("""
                            /afk - Set yourself AFK for max time.
                            /afk time <seconds> - Set yourself AFK for a specific time.
                            /afk players <targets> [time <seconds>] - Set other players AFK for max or specific time."""), false);
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

    /**
     * Simple cooldown map for self-AFK to prevent command spamming.
     */
     private static final java.util.Map<java.util.UUID, Long> afkCooldowns = new java.util.HashMap<>();
    
     /**
     * Cooldown time in milliseconds for self-AFK command.
     */
    private static final long AFK_COOLDOWN_MS = 5000; // 5 seconds

    /**
     * Attempts to put the specified players in AFK mode, with cooldown and combat checks.
     * Provides detailed feedback for each player and broadcasts AFK status.
     *
     * @param source    the command source (for feedback)
     * @param players   the players to set AFK
     * @param timeLimit the AFK time in seconds (-1 for unlimited)
     * @return 1 if at least one player was set AFK, 0 otherwise
     */
    private static int afkPlayers(ServerCommandSource source, Iterable<ServerPlayerEntity> players, double timeLimit) {
        int affected = 0;
        boolean isSelf;
        for (var player : players) {
            LogoutRules rules = (LogoutRules) player;
            isSelf = source.isExecutedByPlayer() && Objects.equals(source.getPlayer(), player);
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
                rules.al_setAllowDisconnectAt(System.currentTimeMillis() + (long) (timeLimit * 1000L));
            }
            rules.al_setAfkDisconnect(true);
            player.networkHandler.disconnect(AntiLogout.AFK_MESSAGE);
            source.sendFeedback(() -> Text.literal("Set " + player.getName().getString() + " AFK for " + (timeLimit == -1 ? "unlimited" : (int) timeLimit) + " seconds."), false);
            if (config.general.debug) AntiLogout.LOGGER.info("[AFK] {} set {} AFK for {} seconds. (combat state: ALLOWED)", source.getName(), player.getName().getString(), (timeLimit == -1 ? "unlimited" : (int) timeLimit));
            Objects.requireNonNull(player.getServer()).getPlayerManager().broadcast(
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
