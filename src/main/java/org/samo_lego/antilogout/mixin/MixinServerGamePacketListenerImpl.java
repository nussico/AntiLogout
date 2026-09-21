package org.samo_lego.antilogout.mixin;

import net.minecraft.network.Connection;
import net.minecraft.network.DisconnectionDetails;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerCommonPacketListenerImpl;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.level.ServerPlayer;
import org.samo_lego.antilogout.AntiLogout;
import org.samo_lego.antilogout.datatracker.LogoutRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class MixinServerGamePacketListenerImpl extends ServerCommonPacketListenerImpl {
    @Shadow
        public ServerPlayer player;

        public MixinServerGamePacketListenerImpl(MinecraftServer minecraftServer, Connection clientConnection,
            CommonListenerCookie connectedClientData) {
        super(minecraftServer, clientConnection, connectedClientData);
    }

    @Shadow
    public abstract ServerPlayer getPlayer();

    /**
     * Injects into the disconnect method to ensure /afk disconnects do not trigger combat log messages or dummies.
     * Cancels disconnect if AFK, otherwise handles as normal.
     * @param disconnectionInfo the disconnection info
     * @param ci callback info
     */
    @Inject(method = "onDisconnect", at = @At("HEAD"), cancellable = true)
    private void al$onDisconnect(DisconnectionDetails disconnectionInfo, CallbackInfo ci) {
        // Generic disconnect is handled by MConnection#al_handleDisconnection
        LogoutRules rules = (LogoutRules) this.getPlayer();
        if (!rules.al_allowDisconnect()
                && disconnectionInfo.reason() == AntiLogout.AFK_MESSAGE) {
            // If this is an AFK disconnect, do not trigger combat log message
            if (rules.al_isAfkDisconnect()) {
                // Reset flag for future disconnects
                rules.al_setAfkDisconnect(false);
                ci.cancel();
                return;
            }
            ((LogoutRules) this.player).al_onRealDisconnect();
            ci.cancel();
        }
    }

    @Inject(method = "onDisconnect", at = @At("TAIL"))
    private void al$removeDisconnectedPlayer(DisconnectionDetails disconnectionInfo, CallbackInfo ci) {
        LogoutRules.DISCONNECTED_PLAYERS.remove(this.player);
    }
}
