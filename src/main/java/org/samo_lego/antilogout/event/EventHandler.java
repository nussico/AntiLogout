package org.samo_lego.antilogout.event;

import me.lucko.fabric.api.permissions.v0.Permissions;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.network.DisconnectionInfo;
import net.minecraft.network.packet.s2c.play.DeathMessageS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.world.World;
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
     * @return {@link ActionResult#PASS} to allow normal event flow
     */
    public static ActionResult onAttack(PlayerEntity attacker, World _level, Hand _interactionHand,
            Entity target, @Nullable EntityHitResult _entityHitResult) {
        if (target instanceof PlayerEntity) {
            long allowedDc = System.currentTimeMillis() + Math.round(AntiLogout.config.combatLog.combatTimeout * 1000L);

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
        return ActionResult.PASS;
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
            ((ServerPlayerEntity) player).networkHandler.onDisconnected(new DisconnectionInfo(Text.empty()));
        }
    }

    /**
     * Marks a player as "in combat state" if the damage source is allowed by config.
     * If the damage source is a projectile shot by a player, the shooter is also marked.
     *
     * @param target       the player who was hurt
     * @param damageSource the damage source
     */
    public static void onHurt(ServerPlayerEntity target, DamageSource damageSource) {
        long allowedDc = System.currentTimeMillis() + Math.round(AntiLogout.config.combatLog.combatTimeout * 1000L);
        if (target != null) {
            boolean trigger;
            if (AntiLogout.config.combatLog.playerHurtOnly) {
                // Only player or player projectile
                trigger = (damageSource.getAttacker() instanceof PlayerEntity) ||
                        (damageSource.getAttacker() instanceof ProjectileEntity p && p.getOwner() instanceof PlayerEntity);
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
     * Sends a stored death message to a player if they died while disconnected but are still present in the world.
     * This ensures the player receives their death message upon rejoining.
     *
     * @param listener the packet listener for the player
     * @param _sender  the packet sender
     * @param _server  the Minecraft server
     */
    public static void onPlayerJoin(ServerPlayNetworkHandler listener, PacketSender _sender,
            MinecraftServer _server) {
        final Text deathMessage = LogoutRules.SKIPPED_DEATH_MESSAGES.get(listener.player.getUuid());
        if (deathMessage != null) {
            listener.player.sendMessage(deathMessage, false);
            listener.sendPacket(new DeathMessageS2CPacket(listener.player.getId(), deathMessage));
            LogoutRules.SKIPPED_DEATH_MESSAGES.remove(listener.player.getUuid());
        }
    }
}
