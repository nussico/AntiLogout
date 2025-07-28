package org.samo_lego.antilogout;

import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.Text;
import org.samo_lego.antilogout.command.AfkCommand;
import org.samo_lego.antilogout.command.AntiLogoutCommand;
import org.samo_lego.antilogout.config.ConfigManager;
import org.samo_lego.antilogout.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AntiLogout implements DedicatedServerModInitializer {
    public static final String MOD_ID = "antilogout";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static ConfigManager.Config config;
    public static final Text AFK_MESSAGE;

    /**
     * Static reference to the current MinecraftServer instance.
     * Used for accessing the server from anywhere in the mod.
     */
    public static MinecraftServer SERVER = null;

    /**
     * Loads the configuration and initializes the AFK message.
     * This static block ensures config is loaded before the mod is initialized.
     */
    static {
        ConfigManager.load();
        config = ConfigManager.config;
        AFK_MESSAGE = Text.translatable(config.afk.afkMessage);
    }

    /**
     * Initializes the AntiLogout mod on the dedicated server.
     * Registers all event listeners and commands, and manages server lifecycle hooks.
     */
    @Override
    public void onInitializeServer() {
        // Register server lifecycle events to track the server instance and cleanup on stop
        ServerLifecycleEvents.SERVER_STARTED.register(server -> SERVER = server);
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            SERVER = null;
            // Clear fake/disconnected players to prevent ghosts after restart
            org.samo_lego.antilogout.datatracker.LogoutRules.DISCONNECTED_PLAYERS.clear();
        });

        // Register event listeners for combat, death, and player join
        AttackEntityCallback.EVENT.register(EventHandler::onAttack);
        ServerLivingEntityEvents.AFTER_DEATH.register(EventHandler::onDeath);
        ServerPlayConnectionEvents.JOIN.register(EventHandler::onPlayerJoin);

        // Register commands for AFK and AntiLogout
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            AfkCommand.register(dispatcher);
            AntiLogoutCommand.register(dispatcher);
        });

        LOGGER.info("AntiLogout initialized.");
    }
}
