package org.samo_lego.antilogout;

import org.samo_lego.antilogout.command.AfkCommand;
import org.samo_lego.antilogout.command.AntiLogoutCommand;
import org.samo_lego.antilogout.config.ConfigManager;
import org.samo_lego.antilogout.datatracker.LogoutRules;
import org.samo_lego.antilogout.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public class AntiLogout implements DedicatedServerModInitializer {
    public static final String MOD_ID = "antilogout";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static ConfigManager.Config config;
    public static final Component AFK_MESSAGE;

    // Static reference to the current MinecraftServer instance
    public static MinecraftServer SERVER = null;

    static {
        ConfigManager.load();
        config = ConfigManager.config;
        AFK_MESSAGE = Component.translatable(config.afk.afkMessage);
    }

    @Override
    public void onInitializeServer() {
        // Register server lifecycle events to track the server instance
        ServerLifecycleEvents.SERVER_STARTED.register(server -> SERVER = server);
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            SERVER = null;
            // Clear fake/disconnected players to prevent ghosts after restart
            org.samo_lego.antilogout.datatracker.LogoutRules.DISCONNECTED_PLAYERS.clear();
        });

        AttackEntityCallback.EVENT.register(EventHandler::onAttack);
        ServerLivingEntityEvents.AFTER_DEATH.register(EventHandler::onDeath);
        ServerPlayConnectionEvents.JOIN.register(EventHandler::onPlayerJoin);

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            AfkCommand.register(dispatcher);
            AntiLogoutCommand.register(dispatcher);
        });

        LOGGER.info("AntiLogout initialized.");
    }

    /**
     * Called after config reload to update all online players' state
     * to reflect new config values (e.g., combat/AFK timers, messages).
     */
    public static void onConfigReload() {
        if (SERVER == null) return;
        if (config.general.debug) LOGGER.info("[DEBUG] Reloading config and updating player states...");
        for (ServerPlayer player : SERVER.getPlayerList().getPlayers()) {
            if (player instanceof LogoutRules rules) {
                long now = System.currentTimeMillis();
                if (!rules.al_allowDisconnect()) {
                    long newCombatTimeout = (long) (config.combatLog.combatTimeout * 1000);
                    long remaining = rules.al_getAllowDisconnectTime() - now;
                    if (remaining < newCombatTimeout) {
                        rules.al_setAllowDisconnectAt(now + newCombatTimeout);
                        if (config.general.debug) LOGGER.info("[DEBUG] Extended combat/AFK timer for {}.", player.getName().getString());
                    }
                }
            }
        }
        LOGGER.info("All online player states updated to reflect new config values.");
        if (config.general.debug) LOGGER.info("[DEBUG] Config reload complete.");
    }
}