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
 * Takes care of events.
 * We could use {@link ServerPlayerEntity#enterCombat()}
 * and {@link ServerPlayerEntity#endCombat()} but
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
     * @return {@link ActionResult#PASS}
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
     * Disconnects afk player on death.
     *
     * @param deadEntity    entity that died
     * @param _damageSource damage source of death
     */
    public static void onDeath(LivingEntity deadEntity, DamageSource _damageSource) {
        if (deadEntity instanceof LogoutRules player && player.al_isFake()) {
            // Remove player from online players
            ((ServerPlayerEntity) player).networkHandler.onDisconnected(new DisconnectionInfo(Text.empty()));
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
     * Sends death message to player if they died while disconnected,
     * but still present in the world.
     *
     * @param listener packet listener
     * @param _sender  packet sender
     * @param _server  minecraft server
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
