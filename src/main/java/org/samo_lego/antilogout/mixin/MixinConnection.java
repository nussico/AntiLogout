package org.samo_lego.antilogout.mixin;

import org.samo_lego.antilogout.datatracker.LogoutRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import io.netty.channel.Channel;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketListener;
import net.minecraft.server.network.ServerGamePacketListenerImpl;

@Mixin(Connection.class)
public abstract class MixinConnection {

    @Shadow
    private Channel channel;

    @Shadow
    public abstract PacketListener getPacketListener();

    /**
     * This method gets called when PLAYER wants to disconnect
     *
     * @param ci
     */
    @Inject(method = "handleDisconnection", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/PacketListener;onDisconnect(Lnet/minecraft/network/DisconnectionDetails;)V"), cancellable = true)
    private void al_handleDisconnection(CallbackInfo ci) {
        if (this.getPacketListener() instanceof ServerGamePacketListenerImpl listener) {
            if (!((LogoutRules) listener.getPlayer()).al_allowDisconnect()) {
                // Broadcast combat logout message
                var player = listener.getPlayer();
                var server = player.getServer();
                if (server != null) {
                    server.getPlayerList().broadcastSystemMessage(
                            net.minecraft.network.chat.Component
                                    .literal(player.getName().getString() + " disconnected while in combat!"),
                            false);
                }
                this.channel.close();
                ((LogoutRules) listener.getPlayer()).al_onRealDisconnect();
                ci.cancel();
            }
        }
    }
}
