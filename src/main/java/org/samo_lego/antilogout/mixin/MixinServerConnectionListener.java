package org.samo_lego.antilogout.mixin;

import net.minecraft.server.ServerNetworkIo;
import net.minecraft.server.network.ServerPlayerEntity;
import org.samo_lego.antilogout.datatracker.LogoutRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerNetworkIo.class)
public class MixinServerConnectionListener {

    /**
     * Injects into the server network tick to tick all players in {@link LogoutRules#DISCONNECTED_PLAYERS}.
     * Ensures dummies/fake players are updated each tick.
     * @param ci callback info
     */
    @Inject(method = "tick", at = @At("TAIL"))
    private void onTickConnections(CallbackInfo ci) {
        // Tick all disconnected/dummy players as well
        LogoutRules.DISCONNECTED_PLAYERS.forEach(ServerPlayerEntity::playerTick);
    }
}
