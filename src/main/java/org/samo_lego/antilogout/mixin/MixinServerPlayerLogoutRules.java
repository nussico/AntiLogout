package org.samo_lego.antilogout.mixin;

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

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.damagesource.DamageSource;

/**
 * Unified mixin for ServerPlayer implementing LogoutRules.
 * Handles disconnect/AFK/combat state and delayed tasks.
 */
@Mixin(ServerPlayer.class)
public abstract class MixinServerPlayerLogoutRules implements LogoutRules {
    @Shadow
    private boolean disconnected;

    @Shadow
    public ServerGamePacketListenerImpl connection;

    @Unique
    private long allowDisconnectTime = 0;
    @Unique
    private boolean executedDisconnect = false;
    @Unique
    private Runnable delayedTask;
    @Unique
    private long taskTime;

    // Determines if the player is allowed to disconnect
    @Override
    public boolean al_allowDisconnect() {
        return this.allowDisconnectTime != -1 && this.allowDisconnectTime <= System.currentTimeMillis()
                && !AntiLogout.config.general.disableAllLogouts;
    }

    // Sets the time when the player can disconnect
    @Override
    public void al_setAllowDisconnectAt(long systemTime) {
        if (AntiLogout.config.general.debug) AntiLogout.LOGGER.info("[COMBAT] Setting allowDisconnectAt for {} to {} ({} seconds from now)", ((ServerPlayer)(Object)this).getName().getString(), systemTime, (systemTime == -1 ? "unlimited" : (systemTime - System.currentTimeMillis())/1000));
        this.allowDisconnectTime = systemTime;
    }

    // Sets whether the player can disconnect immediately
    @Override
    public void al_setAllowDisconnect(boolean allow) {
        this.allowDisconnectTime = allow ? 0 : -1;
    }

    // Returns true if the player is a fake/disconnected entity
    @Override
    public boolean al_isFake() {
        return this.disconnected;
    }

    // Called when the player disconnects for real
    @Override
    public void al_onRealDisconnect() {
        this.disconnected = true;
        if (!this.al_allowDisconnect()) {
            DISCONNECTED_PLAYERS.add((ServerPlayer) (Object) this);
            if (AntiLogout.config.general.debug) AntiLogout.LOGGER.info("[DISCONNECT] {} disconnected while not allowed (combat/AFK).", ((ServerPlayer)(Object)this).getName().getString());
        }
    }

    // Schedules a delayed task (e.g., end of combat message)
    @Override
    public void al$delay(long tickDuration, Runnable task) {
        this.delayedTask = task;
        this.taskTime = tickDuration;
    }

    // Ensures hasDisconnected returns correct value based on our logic
    @Inject(method = "hasDisconnected", at = @At("HEAD"), cancellable = true)
    public void hasDisconnected(CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(this.al_allowDisconnect() && this.disconnected);
    }

    // Handles ticking for fake/disconnected players and delayed tasks
    @Inject(method = "doTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;getInventory()Lnet/minecraft/world/entity/player/Inventory;"), cancellable = true)
    private void onTick(CallbackInfo ci) {
        if (this.al_isFake()) {
            if (this.al_allowDisconnect() && !this.executedDisconnect) {
                this.connection.disconnect(Component.empty());
                this.executedDisconnect = true; // Prevent disconnecting twice
            }
            ci.cancel();
        } else if (this.delayedTask != null && this.taskTime <= System.currentTimeMillis()) {
            this.delayedTask.run();
            this.delayedTask = null;
        }
    }

    // Removes player from DISCONNECTED_PLAYERS on disconnect
    @Inject(method = "disconnect", at = @At("TAIL"))
    private void al_disconnect(CallbackInfo ci) {
        DISCONNECTED_PLAYERS.remove((ServerPlayer) (Object) this);
    }

    // Injects combat/AFK logic on player hurt (from old MixinLogoutRulesPlayer)
    @Inject(method = "hurtServer(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;F)Z", at = @At("TAIL"))
    private void onHurt(ServerLevel serverLevel, DamageSource damageSource, float amount,
            CallbackInfoReturnable<Boolean> cir) {
        EventHandler.onHurt((ServerPlayer) (Object) this, damageSource);
    }

    // Returns the current system time (ms) at which the player is allowed to disconnect
    @Override
    public long al_getAllowDisconnectTime() {
        return this.allowDisconnectTime;
    }
} 