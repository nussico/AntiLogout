package org.samo_lego.antilogout.command;

import static net.minecraft.server.command.CommandManager.literal;

import java.util.concurrent.CompletableFuture;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;

import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

public class AntiLogoutCommand {
    private static final String[] OPTIONS = {
            "disableAllLogouts",
            "combatTimeout",
            "notifyOnCombat",
            "combatEnterMessage",
            "combatEndMessage",
            "playerHurtOnly",
            "bypassPermissionLevel",
            "afkMessage",
            "permissionLevel",
            "maxAfkTime"
    };
    private static final SuggestionProvider<ServerCommandSource> CONFIG_OPTION_SUGGESTIONS = (context, builder) -> {
        for (String opt : OPTIONS) {
            if (opt.startsWith(builder.getRemaining())) {
                builder.suggest(opt);
            }
        }
        return CompletableFuture.completedFuture(builder.build());
    };

    // Maps user-friendly option names to config field accessors
    private static Object getConfigValueByOption(String option, org.samo_lego.antilogout.config.ConfigManager.Config config) {
        switch (option) {
            case "disableAllLogouts":
                return config.general.disableAllLogouts;
            case "combatTimeout":
                return config.combatLog.combatTimeout;
            case "notifyOnCombat":
                return config.combatLog.notifyOnCombat;
            case "combatEnterMessage":
                return config.combatLog.combatEnterMessage;
            case "combatEndMessage":
                return config.combatLog.combatEndMessage;
            case "playerHurtOnly":
                return config.combatLog.playerHurtOnly;
            case "bypassPermissionLevel":
                return config.combatLog.bypassPermissionLevel;
            case "afkMessage":
                return config.afk.afkMessage;
            case "permissionLevel":
                return config.afk.permissionLevel;
            case "maxAfkTime":
                return config.afk.maxAfkTime;
            default:
                return null;
        }
    }

    // Sets config value by user-friendly option name
    private static boolean setConfigValueByOption(String option, String value,
            org.samo_lego.antilogout.config.ConfigManager.Config config) {
        try {
            switch (option) {
                case "disableAllLogouts":
                    config.general.disableAllLogouts = Boolean.parseBoolean(value);
                    return true;
                case "combatTimeout":
                    config.combatLog.combatTimeout = Integer.parseInt(value);
                    return true;
                case "notifyOnCombat":
                    config.combatLog.notifyOnCombat = Boolean.parseBoolean(value);
                    return true;
                case "combatEnterMessage":
                    config.combatLog.combatEnterMessage = value;
                    return true;
                case "combatEndMessage":
                    config.combatLog.combatEndMessage = value;
                    return true;
                case "playerHurtOnly":
                    config.combatLog.playerHurtOnly = Boolean.parseBoolean(value);
                    return true;
                case "bypassPermissionLevel":
                    config.combatLog.bypassPermissionLevel = Integer.parseInt(value);
                    return true;
                case "afkMessage":
                    config.afk.afkMessage = value;
                    return true;
                case "permissionLevel":
                    config.afk.permissionLevel = Integer.parseInt(value);
                    return true;
                case "maxAfkTime":
                    config.afk.maxAfkTime = Double.parseDouble(value);
                    return true;
                default:
                    return false;
            }
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Utility method to format the status message for the config.
     *
     * @param config LogoutConfig
     * @return formatted status string
     */
    private static String formatStatus(org.samo_lego.antilogout.config.ConfigManager.Config config) {
        return "Current AntiLogout Config:\n" +
            "  disableAllLogouts: " + config.general.disableAllLogouts + "\n" +
            "  combatTimeout: " + config.combatLog.combatTimeout + "\n" +
            "  notifyOnCombat: " + config.combatLog.notifyOnCombat + "\n" +
            "  combatEnterMessage: " + config.combatLog.combatEnterMessage + "\n" +
            "  combatEndMessage: " + config.combatLog.combatEndMessage + "\n" +
            "  playerHurtOnly: " + config.combatLog.playerHurtOnly + "\n" +
            "  bypassPermissionLevel: " + config.combatLog.bypassPermissionLevel + "\n" +
            "  afkMessage: " + config.afk.afkMessage + "\n" +
            "  permissionLevel: " + config.afk.permissionLevel + "\n" +
            "  maxAfkTime: " + config.afk.maxAfkTime;
    }

    /**
     * Registers /antilogout command with improved feedback and help.
     * Usage: /antilogout help
     *        /antilogout reload
     *        /antilogout status
     *        /antilogout get <option>
     *        /antilogout set <option> <value>
     * Alias: /al
     *
     * @param dispatcher command dispatcher
     */
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
            CommandManager.literal("antilogout")
                .requires(source -> source.hasPermissionLevel(4))
                .then(CommandManager.literal("help")
                    .executes(ctx -> {
                        ctx.getSource().sendFeedback(() -> Text.literal(
                            "/antilogout reload - Reloads the config file.\n" +
                            "/antilogout status - Shows current config values.\n" +
                            "/antilogout get <option> - Gets a config value.\n" +
                            "/antilogout set <option> <value> - Sets a config value.\n" +
                            "Options: disableAllLogouts, combatTimeout, notifyOnCombat, combatEnterMessage, combatEndMessage, playerHurtOnly, bypassPermissionLevel, afkMessage, permissionLevel, maxAfkTime"
                        ), false);
                        return 1;
                    })
                )
                .then(CommandManager.literal("reload")
                    .requires(source -> source.hasPermissionLevel(2))
                    .executes(ctx -> {
                        org.samo_lego.antilogout.config.ConfigManager.load();
                        ctx.getSource().sendFeedback(() -> Text.literal("AntiLogout config reloaded! (All changes applied immediately.)"), true);
                        return 1;
                    })
                )
                .then(CommandManager.literal("status")
                    .executes(ctx -> {
                        var config = org.samo_lego.antilogout.config.ConfigManager.config;
                        ctx.getSource().sendFeedback(() -> Text.literal(formatStatus(config)), false);
                        return 1;
                    })
                )
                .then(CommandManager.literal("get")
                    .then(CommandManager.argument("option", StringArgumentType.word())
                        .suggests(CONFIG_OPTION_SUGGESTIONS)
                        .executes(ctx -> {
                            var config = org.samo_lego.antilogout.config.ConfigManager.config;
                            String option = StringArgumentType.getString(ctx, "option");
                            Object value = getConfigValueByOption(option, config);
                            if (value == null) {
                                ctx.getSource().sendError(
                                    Text.literal("Unknown option: " + option + ". Use /antilogout help for a list of options."));
                                return 0;
                            }
                            ctx.getSource().sendFeedback(() -> Text.literal(option + ": " + value), false);
                            return 1;
                        })
                    )
                )
                .then(CommandManager.literal("set")
                    .requires(source -> source.hasPermissionLevel(2))
                    .then(CommandManager.argument("option", StringArgumentType.word())
                        .suggests(CONFIG_OPTION_SUGGESTIONS)
                        .then(CommandManager.argument("value", StringArgumentType.greedyString())
                            .suggests((context, builder) -> {
                                String option = StringArgumentType.getString(context, "option");
                                if (option.equals("disableAllLogouts")
                                        || option.equals("notifyOnCombat")
                                        || option.equals("playerHurtOnly")) {
                                    builder.suggest("true");
                                    builder.suggest("false");
                                }
                                return CompletableFuture.completedFuture(builder.build());
                            })
                            .executes(ctx -> {
                                var config = org.samo_lego.antilogout.config.ConfigManager.config;
                                String option = StringArgumentType.getString(ctx, "option");
                                String value = StringArgumentType.getString(ctx, "value");
                                boolean success = setConfigValueByOption(option, value, config);
                                if (success) {
                                    org.samo_lego.antilogout.config.ConfigManager.save();
                                    org.samo_lego.antilogout.config.ConfigManager.load();
                                    ctx.getSource().sendFeedback(
                                        () -> Text.literal("Set " + option + " to " + value + ". (Change applied immediately.)"),
                                        true);
                                    return 1;
                                } else {
                                    ctx.getSource().sendError(
                                        Text.literal("Invalid or unknown value for " + option + ". Use /antilogout help for valid options and value types."));
                                    return 0;
                                }
                            })
                        )
                    )
                )
        );
        // Alias
        dispatcher.register(literal("al").redirect(dispatcher.getRoot().getChild("antilogout")));
    }
}
