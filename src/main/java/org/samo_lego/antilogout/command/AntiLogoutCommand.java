package org.samo_lego.antilogout.command;

import static net.minecraft.commands.Commands.literal;

import java.util.concurrent.CompletableFuture;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

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
    private static final SuggestionProvider<CommandSourceStack> CONFIG_OPTION_SUGGESTIONS = (context, builder) -> {
        for (String opt : OPTIONS) {
            if (opt.startsWith(builder.getRemaining())) {
                builder.suggest(opt);
            }
        }
        return CompletableFuture.completedFuture(builder.build());
    };

    // Maps user-friendly option names to config field accessors
    private static Object getConfigValueByOption(String option, org.samo_lego.antilogout.config.LogoutConfig config) {
        switch (option) {
            case "disableAllLogouts":
                return config.disableAllLogouts;
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
            org.samo_lego.antilogout.config.LogoutConfig config) {
        try {
            switch (option) {
                case "disableAllLogouts":
                    config.disableAllLogouts = Boolean.parseBoolean(value);
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

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("antilogout")
                        .then(Commands.literal("reload")
                                .requires(source -> source.hasPermission(2))
                                .executes(ctx -> {
                                    org.samo_lego.antilogout.AntiLogout.config = org.samo_lego.antilogout.config.LogoutConfig
                                            .load();
                                    ctx.getSource().sendSuccess(() -> Component.literal("AntiLogout config reloaded!"),
                                            true);
                                    return 1;
                                }))
                        .then(Commands.literal("status")
                                .executes(ctx -> {
                                    var config = org.samo_lego.antilogout.AntiLogout.config;
                                    String status = "disableAllLogouts: " + config.disableAllLogouts +
                                            "\ncombatTimeout: " + config.combatLog.combatTimeout +
                                            "\nnotifyOnCombat: " + config.combatLog.notifyOnCombat +
                                            "\ncombatEnterMessage: " + config.combatLog.combatEnterMessage +
                                            "\ncombatEndMessage: " + config.combatLog.combatEndMessage +
                                            "\nplayerHurtOnly: " + config.combatLog.playerHurtOnly +
                                            "\nbypassPermissionLevel: " + config.combatLog.bypassPermissionLevel +
                                            "\nafkMessage: " + config.afk.afkMessage +
                                            "\npermissionLevel: " + config.afk.permissionLevel +
                                            "\nmaxAfkTime: " + config.afk.maxAfkTime;
                                    ctx.getSource().sendSuccess(() -> Component.literal(status), false);
                                    return 1;
                                }))
                        .then(Commands.literal("get")
                                .then(Commands.argument("option", StringArgumentType.word())
                                        .suggests(CONFIG_OPTION_SUGGESTIONS)
                                        .executes(ctx -> {
                                            var config = org.samo_lego.antilogout.AntiLogout.config;
                                            String option = StringArgumentType.getString(ctx, "option");
                                            Object value = getConfigValueByOption(option, config);
                                            if (value == null) {
                                                ctx.getSource().sendFailure(
                                                        Component.literal("Unknown option: " + option));
                                                return 0;
                                            }
                                            ctx.getSource().sendSuccess(() -> Component.literal(option + ": " + value),
                                                    false);
                                            return 1;
                                        })))
                        .then(Commands.literal("set")
                                .requires(source -> source.hasPermission(2))
                                .then(Commands.argument("option", StringArgumentType.word())
                                        .suggests(CONFIG_OPTION_SUGGESTIONS)
                                        .then(Commands.argument("value", StringArgumentType.greedyString())
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
                                                    var config = org.samo_lego.antilogout.AntiLogout.config;
                                                    String option = StringArgumentType.getString(ctx, "option");
                                                    String value = StringArgumentType.getString(ctx, "value");
                                                    boolean success = setConfigValueByOption(option, value, config);
                                                    if (success) {
                                                        config.save();
                                                        org.samo_lego.antilogout.AntiLogout.config = org.samo_lego.antilogout.config.LogoutConfig
                                                                .load();
                                                        ctx.getSource().sendSuccess(
                                                                () -> Component
                                                                        .literal("Set " + option + " to " + value),
                                                                true);
                                                        return 1;
                                                    } else {
                                                        ctx.getSource().sendFailure(
                                                                Component.literal(
                                                                        "Invalid or unknown value for " + option));
                                                        return 0;
                                                    }
                                                })))));
        // Alias
        dispatcher.register(literal("al").redirect(dispatcher.getRoot().getChild("antilogout")));
    }
}
