package org.samo_lego.antilogout.event;

import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.network.chat.Component;
import net.minecraft.network.DisconnectionDetails;
import net.minecraft.network.protocol.game.ClientboundPlayerCombatKillPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.level.ServerPlayer;
import net.fabricmc.fabric.api.permission.v1.PermissionContextOwner;
import net.minecraft.resources.Identifier;
import net.minecraft.server.permissions.PermissionLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.samo_lego.antilogout.AntiLogout;
import org.samo_lego.antilogout.datatracker.LogoutRules;

/**
 * Handles all AntiLogout-related events for combat, AFK, and player state.
 * Uses Fabric events to allow configurable combat timeout and custom logic.
 * We do not use {@link ServerPlayerEntity#enterCombat()} or {@link ServerPlayerEntity#endCombat()} directly
 * because we require more control and configuration than vanilla provides.
 */
public class EventHandler {

    /**
     * Marks both the attacker and the target as "in combat state" if they are players.
     * This is triggered on a player attack event and sets the combat timeout for both parties.
     *
     * @param attacker         the player who attacked
     * @param _level           the world
     * @param _interactionHand the hand used to attack
     * @param target           the targeted entity
     * @param _entityHitResult the hit result
    * @return {@link InteractionResult#PASS} to allow normal event flow
     */
    public static InteractionResult onAttack(Player attacker, Level _level, InteractionHand _interactionHand,
            Entity target, @Nullable EntityHitResult _entityHitResult) {
        if (target instanceof Player) {
            long allowedDc = System.currentTimeMillis() + Math.round(AntiLogout.config.combatLog.combatTimeout * 1000L);

            // Mark target
            if (target instanceof LogoutRules logoutTarget
                    && !hasPermission(target, AntiLogout.config.combatLog.bypassPermissionLevel)) {
                logoutTarget.al_setInCombatUntil(allowedDc);
            }

            // Mark attacker
            if (attacker instanceof LogoutRules logoutAttacker
                        && !hasPermission(attacker, AntiLogout.config.combatLog.bypassPermissionLevel)) {
                logoutAttacker.al_setInCombatUntil(allowedDc);
            }
        }
        return InteractionResult.PASS;
    }

    /**
     * Disconnects a fake (AFK/dummy) player on death.
     * Ensures that fake players are properly removed from the world when they die.
     *
     * @param deadEntity    the entity that died
     * @param _damageSource the damage source of death
     */
    public static void onDeath(LivingEntity deadEntity, DamageSource _damageSource) {
        if (deadEntity instanceof LogoutRules player && player.al_isFake()) {
            // Remove player from online players
            ((ServerPlayer) player).connection.onDisconnect(new DisconnectionDetails(Component.empty()));
        }
    }

    /**
     * Marks a player as "in combat state" if the damage source is allowed by config.
     * If the damage source is a projectile shot by a player, the shooter is also marked.
     *
     * @param target       the player who was hurt
     * @param damageSource the damage source
     */
    public static void onHurt(ServerPlayer target, DamageSource damageSource) {
        long allowedDc = System.currentTimeMillis() + Math.round(AntiLogout.config.combatLog.combatTimeout * 1000L);
        if (target != null) {
            boolean trigger;
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

    private static boolean hasPermission(Entity entity, int level) {
        if (!(entity instanceof ServerPlayer)) return false;
        PermissionContextOwner owner = (PermissionContextOwner) (Object) entity;
        return owner.checkPermission(Identifier.parse("antilogout:bypass.combat"), PermissionLevel.byId(level));
    }

    /**
     * Sends a stored death message to a player if they died while disconnected but are still present in the world.
     * This ensures the player receives their death message upon rejoining.
     *
     * @param listener the packet listener for the player
     * @param _sender  the packet sender
     * @param _server  the Minecraft server
     */
    public static void onPlayerJoin(ServerGamePacketListenerImpl listener, PacketSender _sender,
            MinecraftServer _server) {
        final Component deathMessage = LogoutRules.SKIPPED_DEATH_MESSAGES.get(listener.player.getUUID());
        if (deathMessage != null) {
            listener.player.sendSystemMessage(deathMessage);
            listener.send(new ClientboundPlayerCombatKillPacket(listener.player.getId(), deathMessage));
            LogoutRules.SKIPPED_DEATH_MESSAGES.remove(listener.player.getUUID());
        }
    }
}
