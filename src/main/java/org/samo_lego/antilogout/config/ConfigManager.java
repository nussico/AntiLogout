package org.samo_lego.antilogout.config;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import org.samo_lego.antilogout.AntiLogout;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ConfigManager {
    private static final Path CONFIG_PATH = Paths.get("config", "antilogout.toml");
    public static Config config = new Config();

    static {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
        } catch (IOException e) {
            AntiLogout.LOGGER.info("Could not create config directory!");
        }
    }

    public static void load() {
        CommentedFileConfig configData = CommentedFileConfig.builder(CONFIG_PATH).autosave().build();
        configData.load();
        // General
        config.general.disableAllLogouts = configData.getOrElse("general.disableAllLogouts", config.general.disableAllLogouts);
        config.general.debug = configData.getOrElse("general.debug", config.general.debug);
        // AFK
        config.afk.afkMessage = configData.getOrElse("afk.afkMessage", config.afk.afkMessage);
        config.afk.permissionLevel = configData.getOrElse("afk.permissionLevel", config.afk.permissionLevel);
        config.afk.maxAfkTime = configData.getOrElse("afk.maxAfkTime", config.afk.maxAfkTime);
        config.afk.afkCombatMessage = configData.getOrElse("afk.afkCombatMessage", config.afk.afkCombatMessage);
        config.afk.afkBroadcastMessage = configData.getOrElse("afk.afkBroadcastMessage", config.afk.afkBroadcastMessage);
        // CombatLog
        config.combatLog.notifyOnCombat = configData.getOrElse("combatLog.notifyOnCombat", config.combatLog.notifyOnCombat);
        config.combatLog.combatEnterMessage = configData.getOrElse("combatLog.combatEnterMessage", config.combatLog.combatEnterMessage);
        config.combatLog.combatEndMessage = configData.getOrElse("combatLog.combatEndMessage", config.combatLog.combatEndMessage);
        config.combatLog.combatTimeout = configData.getOrElse("combatLog.combatTimeout", config.combatLog.combatTimeout);
        config.combatLog.playerHurtOnly = configData.getOrElse("combatLog.playerHurtOnly", config.combatLog.playerHurtOnly);
        config.combatLog.bypassPermissionLevel = configData.getOrElse("combatLog.bypassPermissionLevel", config.combatLog.bypassPermissionLevel);
        config.combatLog.combatDisconnectMessage = configData.getOrElse("combatLog.combatDisconnectMessage", config.combatLog.combatDisconnectMessage);
        // Always save to ensure correct structure
        save();
    }

    public static void save() {
        CommentedFileConfig configData = CommentedFileConfig.builder(CONFIG_PATH).autosave().build();
        configData.load();
        // Overwrite with only the correct structure
        configData.clear();
        // General
        configData.setComment("general.disableAllLogouts", "Disable all logout features");
        configData.set("general.disableAllLogouts", config.general.disableAllLogouts);
        configData.setComment("general.debug", "Enable debug logging");
        configData.set("general.debug", config.general.debug);
        // AFK
        configData.setComment("afk.afkMessage", "Message shown when a player is AFK");
        configData.set("afk.afkMessage", config.afk.afkMessage);
        configData.setComment("afk.permissionLevel", "Permission level required for /afk");
        configData.set("afk.permissionLevel", config.afk.permissionLevel);
        configData.setComment("afk.maxAfkTime", "Max AFK time in seconds");
        configData.set("afk.maxAfkTime", config.afk.maxAfkTime);
        configData.setComment("afk.afkCombatMessage", "Message if player tries AFK in combat");
        configData.set("afk.afkCombatMessage", config.afk.afkCombatMessage);
        configData.setComment("afk.afkBroadcastMessage", "Broadcast when player is AFK. {player} = name");
        configData.set("afk.afkBroadcastMessage", config.afk.afkBroadcastMessage);
        // CombatLog
        configData.setComment("combatLog.notifyOnCombat", "Notify on combat");
        configData.set("combatLog.notifyOnCombat", config.combatLog.notifyOnCombat);
        configData.setComment("combatLog.combatEnterMessage", "Message on entering combat");
        configData.set("combatLog.combatEnterMessage", config.combatLog.combatEnterMessage);
        configData.setComment("combatLog.combatEndMessage", "Message on leaving combat");
        configData.set("combatLog.combatEndMessage", config.combatLog.combatEndMessage);
        configData.setComment("combatLog.combatTimeout", "Combat timeout in seconds");
        configData.set("combatLog.combatTimeout", config.combatLog.combatTimeout);
        configData.setComment("combatLog.playerHurtOnly", "Only player damage triggers");
        configData.set("combatLog.playerHurtOnly", config.combatLog.playerHurtOnly);
        configData.setComment("combatLog.bypassPermissionLevel", "Permission to bypass combat log");
        configData.set("combatLog.bypassPermissionLevel", config.combatLog.bypassPermissionLevel);
        configData.setComment("combatLog.combatDisconnectMessage", "Message for combat log disconnect");
        configData.set("combatLog.combatDisconnectMessage", config.combatLog.combatDisconnectMessage);
        configData.save();
    }

    public static class Config {
        public General general = new General();
        public Afk afk = new Afk();
        public CombatLog combatLog = new CombatLog();
        public static class General {
            public boolean disableAllLogouts = false;
            public boolean debug = false;
        }
        public static class Afk {
            public String afkMessage = "You are now AFK!";
            public int permissionLevel = 0;
            public double maxAfkTime = 300.0;
            public String afkCombatMessage = "You disconnected while in combat!";
            public String afkBroadcastMessage = "{player} is now AFK!";
        }
        public static class CombatLog {
            public boolean notifyOnCombat = true;
            public String combatEnterMessage = "You are in combat!";
            public String combatEndMessage = "You are no longer in combat!";
            public int combatTimeout = 30;
            public boolean playerHurtOnly = true;
            public int bypassPermissionLevel = 4;
            public String combatDisconnectMessage = "disconnected while in combat!";
        }
    }
}
