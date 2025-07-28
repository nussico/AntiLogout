package org.samo_lego.antilogout.mixin;

import org.samo_lego.antilogout.AntiLogout;
import org.samo_lego.antilogout.datatracker.LogoutRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.network.ClientConnection;
import net.minecraft.network.DisconnectionInfo;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ConnectedClientData;
import net.minecraft.server.network.ServerCommonNetworkHandler;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;

@Mixin(ServerPlayNetworkHandler.class)
public abstract class MixinServerGamePacketListenerImpl extends ServerCommonNetworkHandler {
    @Shadow
    public ServerPlayerEntity player;

    public MixinServerGamePacketListenerImpl(MinecraftServer minecraftServer, ClientConnection clientConnection,
            ConnectedClientData connectedClientData) {
        super(minecraftServer, clientConnection, connectedClientData);
    }

    @Shadow
    public abstract ServerPlayerEntity getPlayer();

    /**
     * Hooks in the disconnect method, so that /afk works properly
     *
     * @param disconnectionInfo
     * @param ci
     */
    @Inject(method = "onDisconnected", at = @At("HEAD"), cancellable = true)
    private void al$onDisconnect(DisconnectionInfo disconnectionInfo, CallbackInfo ci) {
        // Generic disconnect is handled by MConnection#al_handleDisconnection
        if (!((LogoutRules) this.getPlayer()).al_allowDisconnect()
                && disconnectionInfo.reason() == AntiLogout.AFK_MESSAGE) {
            ((LogoutRules) this.player).al_onRealDisconnect();

            // Disable disconnecting in this case
            ci.cancel();
        }
    }
}
