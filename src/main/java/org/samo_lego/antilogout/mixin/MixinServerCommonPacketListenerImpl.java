package org.samo_lego.antilogout.mixin;

import net.minecraft.network.DisconnectionInfo;
import net.minecraft.server.network.ServerCommonNetworkHandler;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import org.samo_lego.antilogout.datatracker.LogoutRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerCommonNetworkHandler.class)
public class MixinServerCommonPacketListenerImpl {

    /**
     * Injects into the disconnect method to ensure fake/disconnected players are properly handled.
     * Calls onDisconnected for fake players to trigger cleanup logic.
     * @param disconnectionInfo the disconnection info
     * @param ci callback info
     */
    @Inject(method = "disconnect(Lnet/minecraft/network/DisconnectionInfo;)V", at = @At("TAIL"))
    private void al$disconnect(DisconnectionInfo disconnectionInfo, CallbackInfo ci) {
        if (((Object) this) instanceof ServerPlayNetworkHandler serverGamePacketListener) {
            if (((LogoutRules) serverGamePacketListener.player).al_isFake()) {
                serverGamePacketListener.onDisconnected(disconnectionInfo);
            }
        }
    }
}
