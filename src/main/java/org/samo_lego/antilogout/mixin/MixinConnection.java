package org.samo_lego.antilogout.mixin;

import io.netty.channel.Channel;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketListener;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.samo_lego.antilogout.datatracker.LogoutRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Connection.class)
public abstract class MixinConnection {

    @Shadow
    private Channel channel;

    @Shadow
    public abstract PacketListener getPacketListener();

    /**
     * Injects into the player disconnect handler to manage combat log and AFK disconnects.
     * Suppresses combat log message if disconnect is AFK-triggered.
     * @param ci callback info
     */
    @Inject(method = "handleDisconnection", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/PacketListener;onDisconnect(Lnet/minecraft/network/DisconnectionDetails;)V"), cancellable = true)
    private void al_handleDisconnection(CallbackInfo ci) {
        if (this.getPacketListener() instanceof ServerGamePacketListenerImpl listener) {
            LogoutRules rules = (LogoutRules) listener.getPlayer();
            if (!rules.al_allowDisconnect()) {
                // Suppress combat log message if AFK disconnect
                if (!rules.al_isAfkDisconnect()) {
                    var player = listener.getPlayer();
                    var server = player.level().getServer();
                    if (server != null) {
                        server.getPlayerList().broadcastSystemMessage(
                                net.minecraft.network.chat.Component
                                        .literal(player.getName().getString() + " " + org.samo_lego.antilogout.AntiLogout.config.combatLog.combatDisconnectMessage),
                                false);
                    }
                }
                this.channel.close();
                rules.al_onRealDisconnect();
                ci.cancel();
            }
        }
    }
}
