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
    private static final SuggestionProvider<CommandSourceStack> CONFIG_OPTION_SUGGESTIONS = (context, builder) -> {
        String[] options = { "combatTimeout", "afkMessage", "maxAfkTime", "disableAllLogouts" };
        for (String opt : options) {
            if (opt.startsWith(builder.getRemaining())) {
                builder.suggest(opt);
            }
        }
        return CompletableFuture.completedFuture(builder.build());
    };

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
                                    ctx.getSource().sendSuccess(() -> Component.literal(
                                            "disableAllLogouts: " + config.disableAllLogouts +
                                                    "\ncombatTimeout: " + config.combatLog.combatTimeout +
                                                    "\nafkMessage: " + config.afk.afkMessage +
                                                    "\nmaxAfkTime: " + config.afk.maxAfkTime),
                                            false);
                                    return 1;
                                }))
                        .then(Commands.literal("get")
                                .then(Commands.argument("option", StringArgumentType.word())
                                        .suggests(CONFIG_OPTION_SUGGESTIONS)
                                        .executes(ctx -> {
                                            var config = org.samo_lego.antilogout.AntiLogout.config;
                                            String option = StringArgumentType.getString(ctx, "option");
                                            String value;
                                            switch (option) {
                                                case "combatTimeout":
                                                    value = String.valueOf(config.combatLog.combatTimeout);
                                                    break;
                                                case "afkMessage":
                                                    value = config.afk.afkMessage;
                                                    break;
                                                case "maxAfkTime":
                                                    value = String.valueOf(config.afk.maxAfkTime);
                                                    break;
                                                case "disableAllLogouts":
                                                    value = String.valueOf(config.disableAllLogouts);
                                                    break;
                                                default:
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
                                                .executes(ctx -> {
                                                    var config = org.samo_lego.antilogout.AntiLogout.config;
                                                    String option = StringArgumentType.getString(ctx, "option");
                                                    String value = StringArgumentType.getString(ctx, "value");
                                                    boolean success = true;
                                                    switch (option) {
                                                        case "combatTimeout":
                                                            try {
                                                                config.combatLog.combatTimeout = Integer
                                                                        .parseInt(value);
                                                            } catch (Exception e) {
                                                                success = false;
                                                            }
                                                            break;
                                                        case "afkMessage":
                                                            config.afk.afkMessage = value;
                                                            break;
                                                        case "maxAfkTime":
                                                            try {
                                                                config.afk.maxAfkTime = Double.parseDouble(value);
                                                            } catch (Exception e) {
                                                                success = false;
                                                            }
                                                            break;
                                                        case "disableAllLogouts":
                                                            config.disableAllLogouts = Boolean.parseBoolean(value);
                                                            break;
                                                        default:
                                                            ctx.getSource().sendFailure(
                                                                    Component.literal("Unknown option: " + option));
                                                            return 0;
                                                    }
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
                                                                Component.literal("Invalid value for " + option));
                                                        return 0;
                                                    }
                                                })))));
        // Alias
        dispatcher.register(literal("al").redirect(dispatcher.getRoot().getChild("antilogout")));
    }
}
