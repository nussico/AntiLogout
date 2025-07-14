package org.samo_lego.antilogout.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;

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
        public boolean playerHurtOnly = true;
        public int bypassPermissionLevel = 4;
    }

    public static LogoutConfig load() {
        Path configPath = Paths.get("config", "antilogout.toml");
        // --- Merge duplicate sections before loading ---
        if (Files.exists(configPath)) {
            try {
                String content = Files.readString(configPath);
                String merged = mergeDuplicateTomlSections(content, "afk");
                merged = mergeDuplicateTomlSections(merged, "combatLog");
                Files.writeString(configPath, merged);
            } catch (IOException ignored) {
            }
        }
        // --- End merge logic ---
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
        config.combatLog.playerHurtOnly = configData.getOrElse("combatLog.playerHurtOnly", true);
        config.combatLog.bypassPermissionLevel = configData.getOrElse("combatLog.bypassPermissionLevel", 4);

        return config;
    }

    public void save() {
        Path configPath = Paths.get("config", "antilogout.toml");
        // --- Merge duplicate sections before saving ---
        if (Files.exists(configPath)) {
            try {
                String content = Files.readString(configPath);
                String merged = mergeDuplicateTomlSections(content, "afk");
                merged = mergeDuplicateTomlSections(merged, "combatLog");
                Files.writeString(configPath, merged);
            } catch (IOException ignored) {
            }
        }
        // --- End merge logic ---
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

    // Merges duplicate TOML sections into one, keeping only the last value for each
    // key
    private static String mergeDuplicateTomlSections(String toml, String section) {
        String[] lines = toml.split("\\R");
        Map<String, String> merged = new LinkedHashMap<>();
        boolean inSection = false;
        for (String line : lines) {
            if (line.trim().equals("[" + section + "]")) {
                inSection = true;
                continue;
            }
            if (line.startsWith("[")) {
                inSection = false;
            }
            if (inSection && line.contains("=")) {
                String[] kv = line.split("=", 2);
                String key = kv[0].trim();
                String value = kv[1].trim();
                // Always overwrite, so the last value is kept
                merged.put(key, value);
            }
        }
        // Remove all [section] blocks and their keys
        StringBuilder sb = new StringBuilder();
        boolean skip = false;
        for (String line : lines) {
            if (line.trim().equals("[" + section + "]")) {
                skip = true;
                continue;
            }
            if (skip && (line.startsWith("[") || line.isBlank())) {
                skip = false;
            }
            if (!skip)
                sb.append(line).append("\n");
        }
        // Add merged section at the end
        if (!merged.isEmpty()) {
            sb.append("[" + section + "]\n");
            for (var entry : merged.entrySet()) {
                sb.append(entry.getKey()).append(" = ").append(entry.getValue()).append("\n");
            }
        }
        return sb.toString();
    }
}
