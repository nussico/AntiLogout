package org.samo_lego.antilogout.config;

import java.nio.file.Path;
import java.nio.file.Paths;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;

public class LogoutConfig {
    public AfkConfig afk = new AfkConfig();
    public CombatLogConfig combatLog = new CombatLogConfig();
    public boolean disableAllLogouts = false;

    public static class AfkConfig {
        public String afkMessage = "You are now AFK!";
        public int permissionLevel = 2;
        public double maxAfkTime = 300;
    }

    public static class CombatLogConfig {
        public boolean notifyOnCombat = true;
        public String combatEnterMessage = "You are in combat!";
        public String combatEndMessage = "You are no longer in combat!";
        public int combatTimeout = 30;
        public boolean playerHurtOnly = false;
        public int bypassPermissionLevel = 4;
    }

    public static LogoutConfig load() {
        Path configPath = Paths.get("config", "antilogout.toml");
        CommentedFileConfig configData = CommentedFileConfig.builder(configPath).autosave().build();
        configData.load();

        // If the config file is empty, write defaults and reload
        if (configData.entrySet().isEmpty()) {
            LogoutConfig defaultConfig = new LogoutConfig();
            defaultConfig.save();
            configData.load();
        }

        LogoutConfig config = new LogoutConfig();
        config.disableAllLogouts = configData.getOrElse("disableAllLogouts", false);

        // AFK section
        config.afk.afkMessage = configData.getOrElse("afk.afkMessage", "You are now AFK!");
        config.afk.permissionLevel = configData.getOrElse("afk.permissionLevel", 2);
        config.afk.maxAfkTime = configData.getOrElse("afk.maxAfkTime", 300.0);

        // CombatLog section
        config.combatLog.notifyOnCombat = configData.getOrElse("combatLog.notifyOnCombat", true);
        config.combatLog.combatEnterMessage = configData.getOrElse("combatLog.combatEnterMessage",
                "You are in combat!");
        config.combatLog.combatEndMessage = configData.getOrElse("combatLog.combatEndMessage",
                "You are no longer in combat!");
        config.combatLog.combatTimeout = configData.getOrElse("combatLog.combatTimeout", 30);
        config.combatLog.playerHurtOnly = configData.getOrElse("combatLog.playerHurtOnly", false);
        config.combatLog.bypassPermissionLevel = configData.getOrElse("combatLog.bypassPermissionLevel", 4);

        return config;
    }

    public void save() {
        Path configPath = Paths.get("config", "antilogout.toml");
        CommentedFileConfig configData = CommentedFileConfig.builder(configPath).autosave().build();
        configData.load();
        configData.set("disableAllLogouts", this.disableAllLogouts);
        // AFK section
        configData.set("afk.afkMessage", this.afk.afkMessage);
        configData.set("afk.permissionLevel", this.afk.permissionLevel);
        configData.set("afk.maxAfkTime", this.afk.maxAfkTime);
        // CombatLog section
        configData.set("combatLog.notifyOnCombat", this.combatLog.notifyOnCombat);
        configData.set("combatLog.combatEnterMessage", this.combatLog.combatEnterMessage);
        configData.set("combatLog.combatEndMessage", this.combatLog.combatEndMessage);
        configData.set("combatLog.combatTimeout", this.combatLog.combatTimeout);
        configData.set("combatLog.playerHurtOnly", this.combatLog.playerHurtOnly);
        configData.set("combatLog.bypassPermissionLevel", this.combatLog.bypassPermissionLevel);
        configData.save();
    }
}
