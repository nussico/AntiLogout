package org.samo_lego.antilogout.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import org.samo_lego.antilogout.AntiLogout;
import net.minecraft.commands.Commands;
import net.minecraft.commands.CommandSourceStack;
import net.fabricmc.fabric.api.permission.v1.PermissionContextOwner;
import net.minecraft.resources.Identifier;
import net.minecraft.server.permissions.PermissionLevel;
import net.minecraft.network.chat.Component;

import java.util.concurrent.CompletableFuture;

import static net.minecraft.commands.Commands.literal;

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
    private static Object getConfigValueByOption(String option, org.samo_lego.antilogout.config.ConfigManager.Config config) {
        return switch (option) {
            case "disableAllLogouts" -> config.general.disableAllLogouts;
            case "combatTimeout" -> config.combatLog.combatTimeout;
            case "notifyOnCombat" -> config.combatLog.notifyOnCombat;
            case "combatEnterMessage" -> config.combatLog.combatEnterMessage;
            case "combatEndMessage" -> config.combatLog.combatEndMessage;
            case "playerHurtOnly" -> config.combatLog.playerHurtOnly;
            case "bypassPermissionLevel" -> config.combatLog.bypassPermissionLevel;
            case "afkMessage" -> config.afk.afkMessage;
            case "permissionLevel" -> config.afk.permissionLevel;
            case "maxAfkTime" -> config.afk.maxAfkTime;
            default -> null;
        };
    }

    private static boolean hasPermission(CommandSourceStack source, String permission, int level) {
        PermissionContextOwner owner = (PermissionContextOwner) (Object) source;
        return owner.checkPermission(Identifier.parse(permission.replaceFirst("\\.", ":")), PermissionLevel.byId(level));
    }

    private static Boolean parseBoolean(String value) {
        if (value.equalsIgnoreCase("true")) return true;
        if (value.equalsIgnoreCase("false")) return false;
        return null;
    }

    private static Integer parseInteger(String value, int minimum, int maximum) {
        try {
            int parsed = Integer.parseInt(value);
            return parsed >= minimum && parsed <= maximum ? parsed : null;
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private static Double parseDouble(String value, double minimum) {
        try {
            double parsed = Double.parseDouble(value);
            return Double.isFinite(parsed) && parsed >= minimum ? parsed : null;
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    // Sets config value by user-friendly option name
    private static boolean setConfigValueByOption(String option, String value,
            org.samo_lego.antilogout.config.ConfigManager.Config config) {
        try {
            return switch (option) {
                case "disableAllLogouts" -> {
                    Boolean parsed = parseBoolean(value);
                    if (parsed == null) yield false;
                    config.general.disableAllLogouts = parsed;
                    yield true;
                }
                case "combatTimeout" -> {
                    Integer parsed = parseInteger(value, 0, Integer.MAX_VALUE);
                    if (parsed == null) yield false;
                    config.combatLog.combatTimeout = parsed;
                    yield true;
                }
                case "notifyOnCombat" -> {
                    Boolean parsed = parseBoolean(value);
                    if (parsed == null) yield false;
                    config.combatLog.notifyOnCombat = parsed;
                    yield true;
                }
                case "combatEnterMessage" -> {
                    config.combatLog.combatEnterMessage = value;
                    yield true;
                }
                case "combatEndMessage" -> {
                    config.combatLog.combatEndMessage = value;
                    yield true;
                }
                case "playerHurtOnly" -> {
                    Boolean parsed = parseBoolean(value);
                    if (parsed == null) yield false;
                    config.combatLog.playerHurtOnly = parsed;
                    yield true;
                }
                case "bypassPermissionLevel" -> {
                    Integer parsed = parseInteger(value, 0, 4);
                    if (parsed == null) yield false;
                    config.combatLog.bypassPermissionLevel = parsed;
                    yield true;
                }
                case "afkMessage" -> {
                    config.afk.afkMessage = value;
                    yield true;
                }
                case "permissionLevel" -> {
                    Integer parsed = parseInteger(value, 0, 4);
                    if (parsed == null) yield false;
                    config.afk.permissionLevel = parsed;
                    yield true;
                }
                case "maxAfkTime" -> {
                    Double parsed = parseDouble(value, -1);
                    if (parsed == null) yield false;
                    config.afk.maxAfkTime = parsed;
                    yield true;
                }
                default -> false;
            };
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Formats the status message for the current AntiLogout config.
     * Lists all relevant config values in a readable format.
     *
     * @param config the current AntiLogout config
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
     * Registers the /antilogout command and all its subcommands.
     *
     * Usage:
     *   /antilogout help - Show usage info.
     *   /antilogout reload - Reloads the config file.
     *   /antilogout status - Shows current config values.
     *   /antilogout get <option> - Gets a config value.
     *   /antilogout set <option> <value> - Sets a config value.
     * Alias: /al
     *
     * @param dispatcher the command dispatcher
     */
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("antilogout")
                .requires(source -> hasPermission(source, "antilogout.command.antilogout", 4))
                .then(Commands.literal("help")
                    .executes(ctx -> {
                        ctx.getSource().sendSuccess(() -> Component.literal(
                                """
                                        /antilogout reload - Reloads the config file.
                                        /antilogout status - Shows current config values.
                                        /antilogout get <option> - Gets a config value.
                                        /antilogout set <option> <value> - Sets a config value.
                                        Options: disableAllLogouts, combatTimeout, notifyOnCombat, combatEnterMessage, combatEndMessage, playerHurtOnly, bypassPermissionLevel, afkMessage, permissionLevel, maxAfkTime"""
                        ), false);
                        return 1;
                    })
                )
                .then(Commands.literal("reload")
                    .requires(source -> hasPermission(source, "antilogout.command.antilogout.reload", 4))
                    .executes(ctx -> {
                        org.samo_lego.antilogout.config.ConfigManager.load();
                        AntiLogout.refreshAfkMessage();
                        ctx.getSource().sendSuccess(() -> Component.literal("AntiLogout config reloaded! (All changes applied immediately.)"), true);
                        return 1;
                    })
                )
                .then(Commands.literal("status")
                    .executes(ctx -> {
                        var config = org.samo_lego.antilogout.config.ConfigManager.config;
                        ctx.getSource().sendSuccess(() -> Component.literal(formatStatus(config)), false);
                        return 1;
                    })
                )
                .then(Commands.literal("get")
                    .then(Commands.argument("option", StringArgumentType.word())
                        .suggests(CONFIG_OPTION_SUGGESTIONS)
                        .executes(ctx -> {
                            var config = org.samo_lego.antilogout.config.ConfigManager.config;
                            String option = StringArgumentType.getString(ctx, "option");
                            Object value = getConfigValueByOption(option, config);
                            if (value == null) {
                                ctx.getSource().sendFailure(
                                    Component.literal("Unknown option: " + option + ". Use /antilogout help for a list of options."));
                                return 0;
                            }
                            ctx.getSource().sendSuccess(() -> Component.literal(option + ": " + value), false);
                            return 1;
                        })
                    )
                )
                .then(Commands.literal("set")
                    .requires(source -> hasPermission(source, "antilogout.command.antilogout.edit", 4))
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
                                var config = org.samo_lego.antilogout.config.ConfigManager.config;
                                String option = StringArgumentType.getString(ctx, "option");
                                String value = StringArgumentType.getString(ctx, "value");
                                boolean success = setConfigValueByOption(option, value, config);
                                if (success) {
                                    org.samo_lego.antilogout.config.ConfigManager.save();
                                    org.samo_lego.antilogout.config.ConfigManager.load();
                                    AntiLogout.refreshAfkMessage();
                                    ctx.getSource().sendSuccess(
                                        () -> Component.literal("Set " + option + " to " + value + ". (Change applied immediately.)"),
                                        true);
                                    return 1;
                                } else {
                                    ctx.getSource().sendFailure(
                                        Component.literal("Invalid or unknown value for " + option + ". Use /antilogout help for valid options and value types."));
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
