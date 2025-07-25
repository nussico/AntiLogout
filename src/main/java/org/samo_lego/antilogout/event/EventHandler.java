package org.samo_lego.antilogout.event;

import org.jetbrains.annotations.Nullable;
import org.samo_lego.antilogout.AntiLogout;
import org.samo_lego.antilogout.datatracker.LogoutRules;

import me.lucko.fabric.api.permissions.v0.Permissions;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.network.DisconnectionDetails;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundPlayerCombatKillPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;

/**
 * Takes care of events.
 * We could use {@link ServerPlayer#onEnterCombat()}
 * and {@link ServerPlayer#onLeaveCombat()} but
 * since we want configurable combat timeout, we
 * have to use fabric events.
 */
public class EventHandler {

    /**
     * Marks attacker and target as "in combat state".
     *
     * @param attacker         player who attacked
     * @param _level           world
     * @param _interactionHand hand used to attack
     * @param target           targeted entity
     * @param _entityHitResult hit result
     * @return {@link InteractionResult#PASS}
     */
    public static InteractionResult onAttack(Player attacker, Level _level, InteractionHand _interactionHand,
            Entity target, @Nullable EntityHitResult _entityHitResult) {
        if (target instanceof Player) {
            long allowedDc = System.currentTimeMillis() + Math.round(AntiLogout.config.combatLog.combatTimeout * 1000);

            // Mark target
            if (target instanceof LogoutRules logoutTarget
                    && !Permissions.check(target, "antilogout.bypass.combat", AntiLogout.config.combatLog.bypassPermissionLevel)) {
                logoutTarget.al_setInCombatUntil(allowedDc);
            }

            // Mark attacker
            if (attacker instanceof LogoutRules logoutAttacker
                    && !Permissions.check(attacker, "antilogout.bypass.combat",
                            AntiLogout.config.combatLog.bypassPermissionLevel)) {
                logoutAttacker.al_setInCombatUntil(allowedDc);
            }
        }
        return InteractionResult.PASS;
    }

    /**
     * Disconnects afk player on death.
     *
     * @param deadEntity    entity that died
     * @param _damageSource damage source of death
     */
    public static void onDeath(LivingEntity deadEntity, DamageSource _damageSource) {
        if (deadEntity instanceof LogoutRules player && player.al_isFake()) {
            // Remove player from online players
            ((ServerPlayer) player).connection.onDisconnect(new DisconnectionDetails(Component.empty()));
        }
    }

    /**
     * Marks player as "in combat state" if
     * enabled for that damage source.
     * If damage source is a projectile, shot by
     * a player, then that player is also marked.
     *
     * @param target       player who was hurt
     * @param damageSource damage source
     */
    public static void onHurt(ServerPlayer target, DamageSource damageSource) {
        long allowedDc = System.currentTimeMillis() + Math.round(AntiLogout.config.combatLog.combatTimeout * 1000);
        if (target instanceof Player) {
            boolean trigger = false;
            if (AntiLogout.config.combatLog.playerHurtOnly) {
                // Only player or player projectile
                trigger = (damageSource.getEntity() instanceof Player) ||
                        (damageSource.getEntity() instanceof Projectile p && p.getOwner() instanceof Player);
            } else {
                // Any damage triggers
                trigger = true;
            }
            if (trigger) {
                ((LogoutRules) target).al_setInCombatUntil(allowedDc);
            }
        }
    }

    /**
     * Sends death message to player if they died while disconnected,
     * but still present in the world.
     *
     * @param listener packet listener
     * @param _sender  packet sender
     * @param _server  minecraft server
     */
    public static void onPlayerJoin(ServerGamePacketListenerImpl listener, PacketSender _sender,
            MinecraftServer _server) {
        final Component deathMessage = LogoutRules.SKIPPED_DEATH_MESSAGES.get(listener.player.getUUID());
        if (deathMessage != null) {
            listener.player.displayClientMessage(deathMessage, false);
            listener.send(new ClientboundPlayerCombatKillPacket(listener.player.getId(), deathMessage));
            LogoutRules.SKIPPED_DEATH_MESSAGES.remove(listener.player.getUUID());
        }
    }
}
