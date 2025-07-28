package org.samo_lego.antilogout.mixin;

import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import org.samo_lego.antilogout.AntiLogout;
import org.samo_lego.antilogout.datatracker.LogoutRules;
import org.samo_lego.antilogout.event.EventHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin for ServerPlayerEntity implementing LogoutRules.
 * Handles disconnect, AFK, combat state, and delayed tasks for AntiLogout.
 */
@Mixin(ServerPlayerEntity.class)
public abstract class MixinServerPlayerLogoutRules implements LogoutRules {
    @Shadow
    private boolean disconnected;

    @Shadow
    public ServerPlayNetworkHandler networkHandler;

    @Unique
    private long allowDisconnectTime = 0;
    @Unique
    private boolean executedDisconnect = false;
    @Unique
    private Runnable delayedTask;
    @Unique
    private long taskTime;
    @Unique
    private boolean afkDisconnect = false;
    @Override
    public void al_setAfkDisconnect(boolean afk) {
        this.afkDisconnect = afk;
    }

    @Override
    public boolean al_isAfkDisconnect() {
        return this.afkDisconnect;
    }

    /**
     * Checks if the player is currently allowed to disconnect.
     * @return true if allowed, false otherwise
     */
    @Override
    public boolean al_allowDisconnect() {
        return this.allowDisconnectTime != -1 && this.allowDisconnectTime <= System.currentTimeMillis()
                && !AntiLogout.config.general.disableAllLogouts;
    }

    /**
     * Sets the system time (in ms) when the player is allowed to disconnect.
     * @param systemTime time in milliseconds when disconnect is allowed
     */
    @Override
    public void al_setAllowDisconnectAt(long systemTime) {
        if (AntiLogout.config.general.debug) AntiLogout.LOGGER.info("[COMBAT] Setting allowDisconnectAt for {} to {} ({} seconds from now)", ((ServerPlayerEntity)(Object)this).getName().getString(), systemTime, (systemTime == -1 ? "unlimited" : (systemTime - System.currentTimeMillis())/1000));
        this.allowDisconnectTime = systemTime;
    }

    /**
     * Sets whether the player can disconnect immediately.
     * @param allow true to allow immediate disconnect, false otherwise
     */
    @Override
    public void al_setAllowDisconnect(boolean allow) {
        this.allowDisconnectTime = allow ? 0 : -1;
    }

    /**
     * Checks if the player is a fake/disconnected entity (present in the world, but not connected).
     * @return true if fake, false otherwise
     */
    @Override
    public boolean al_isFake() {
        return this.disconnected;
    }

    /**
     * Called when the player disconnects for real (not a soft/fake disconnect).
     */
    @Override
    public void al_onRealDisconnect() {
        this.disconnected = true;
        // If this is an AFK disconnect, do not create a dummy or log combat/AFK disconnect
        if (!this.al_allowDisconnect() && !this.al_isAfkDisconnect()) {
            DISCONNECTED_PLAYERS.add((ServerPlayerEntity) (Object) this);
            if (AntiLogout.config.general.debug) AntiLogout.LOGGER.info("[DISCONNECT] {} disconnected while not allowed (combat/AFK).", ((ServerPlayerEntity)(Object)this).getName().getString());
        }
        // Always reset AFK flag after any disconnect
        if (this.al_isAfkDisconnect()) {
            this.al_setAfkDisconnect(false);
        }
    }

    /**
     * Schedules a delayed task (e.g., end of combat message).
     * @param tickDuration system time in ms when the task should be executed
     * @param task the task to execute
     */
    @Override
    public void al$delay(long tickDuration, Runnable task) {
        this.delayedTask = task;
        this.taskTime = tickDuration;
    }

    /**
     * Ensures isDisconnected returns correct value based on AntiLogout logic.
     * @param cir callback info for return value
     */
    @Inject(method = "isDisconnected", at = @At("HEAD"), cancellable = true)
    public void isDisconnected(CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(this.al_allowDisconnect() && this.disconnected);
    }

    /**
     * Handles ticking for fake/disconnected players and delayed tasks.
     * Cancels tick if player is fake/disconnected.
     * @param ci callback info
     */
    @Inject(method = "playerTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/network/ServerPlayerEntity;getInventory()Lnet/minecraft/entity/player/PlayerInventory;"), cancellable = true)
    private void playerTick(CallbackInfo ci) {
        if (this.al_isFake()) {
            if (this.al_allowDisconnect() && !this.executedDisconnect) {
                this.networkHandler.disconnect(Text.empty());
                this.executedDisconnect = true; // Prevent disconnecting twice
            }
            ci.cancel();
        } else if (this.delayedTask != null && this.taskTime <= System.currentTimeMillis()) {
            this.delayedTask.run();
            this.delayedTask = null;
        }
    }

    /**
     * Removes player from DISCONNECTED_PLAYERS on disconnect.
     * @param ci callback info
     */
    @Inject(method = "onDisconnect", at = @At("TAIL"))
    private void al_onDisconnect(CallbackInfo ci) {
        DISCONNECTED_PLAYERS.remove((ServerPlayerEntity) (Object) this);
    }

    /**
     * Injects combat/AFK logic on player hurt (from old MixinLogoutRulesPlayer).
     * @param serverWorld the server world
     * @param damageSource the damage source
     * @param amount the amount of damage
     * @param cir callback info for return value
     */
    @Inject(method = "damage(Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/entity/damage/DamageSource;F)Z", at = @At("TAIL"))
    private void onHurt(ServerWorld serverWorld, DamageSource damageSource, float amount,
            CallbackInfoReturnable<Boolean> cir) {
        EventHandler.onHurt((ServerPlayerEntity) (Object) this, damageSource);
    }

    /**
     * Returns the current system time (ms) at which the player is allowed to disconnect.
     * @return system time in ms when disconnect is allowed
     */
    @Override
    public long al_getAllowDisconnectTime() {
        return this.allowDisconnectTime;
    }
}
