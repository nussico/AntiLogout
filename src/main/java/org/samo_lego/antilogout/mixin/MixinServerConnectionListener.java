package org.samo_lego.antilogout.mixin;

import org.samo_lego.antilogout.datatracker.LogoutRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.server.ServerNetworkIo;
import net.minecraft.server.network.ServerPlayerEntity;

@Mixin(ServerNetworkIo.class)
public class MixinServerConnectionListener {

    /**
     * Ticks all the players that are in {@link LogoutRules#DISCONNECTED_PLAYERS}
     *
     * @param ci
     */
    @Inject(method = "tick", at = @At("TAIL"))
    private void onTickConnections(CallbackInfo ci) {
        // Tick "disconnected" players as well
        LogoutRules.DISCONNECTED_PLAYERS.forEach(ServerPlayerEntity::playerTick);
    }
}
