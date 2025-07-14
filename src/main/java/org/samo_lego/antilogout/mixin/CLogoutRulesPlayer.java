package org.samo_lego.antilogout.mixin;

import org.samo_lego.antilogout.AntiLogout;
import org.samo_lego.antilogout.datatracker.ILogoutRules;
import org.samo_lego.antilogout.event.EventHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

/**
 * Implements {@link ILogoutRules} for {@link ServerPlayer}.
 */
@Mixin(LivingEntity.class)
public abstract class CLogoutRulesPlayer implements ILogoutRules {
    @Unique
    private long allowDisconnectTime = 0;
    @Unique
    private boolean disconnected;

    @Override
    public boolean al_allowDisconnect() {
        return this.allowDisconnectTime != -1 && this.allowDisconnectTime <= System.currentTimeMillis()
                && !AntiLogout.config.disableAllLogouts;
    }

    @Override
    public void al_setAllowDisconnectAt(long systemTime) {
        this.allowDisconnectTime = systemTime;
    }

    @Override
    public void al_setAllowDisconnect(boolean allow) {
        this.allowDisconnectTime = allow ? 0 : -1;
    }

    @Override
    public boolean al_isFake() {
        return this.disconnected;
    }

    @Override
    public void al_onRealDisconnect() {
        this.disconnected = true;

        if (!this.al_allowDisconnect()) {
            DISCONNECTED_PLAYERS.add((ServerPlayer) (Object) this);
        }
    }

    @Inject(method = "hurtServer(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;F)Z", at = @At("TAIL"))
    private void onHurt(ServerLevel serverLevel, DamageSource damageSource, float amount,
            CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof ServerPlayer serverPlayer) {
            EventHandler.onHurt(serverPlayer, damageSource);
        }
    }
}
