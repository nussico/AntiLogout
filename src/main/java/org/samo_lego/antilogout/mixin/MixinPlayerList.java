package org.samo_lego.antilogout.mixin;

import java.util.List;

import org.samo_lego.antilogout.datatracker.LogoutRules;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.network.Connection;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.players.PlayerList;

/**
 * Kicks same players that are in {@link LogoutRules#DISCONNECTED_PLAYERS} list
 * when player with same UUID joins.
 */
@Mixin(PlayerList.class)
public abstract class MixinPlayerList {

    @Shadow
    @Final
    private MinecraftServer server;

    @Shadow
    public abstract List<ServerPlayer> getPlayers();

    /**
     * When a player wants to connect but is still online,
     * we allow players with same uuid to be disconnected.
     */
    @Inject(method = "placeNewPlayer(Lnet/minecraft/network/Connection;Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/server/network/CommonListenerCookie;)V", at = @At("HEAD"))
    private void onPlaceNewPlayer(Connection connection, ServerPlayer serverPlayer, CommonListenerCookie cookie,
            CallbackInfo ci) {
        var matchingPlayers = getPlayers().stream().filter(player -> player.getUUID().equals(serverPlayer.getUUID()))
                .toList();

        for (ServerPlayer player : matchingPlayers) {
            // Allows disconnect
            ((LogoutRules) player).al_setAllowDisconnect(true);

            // Removes player so that the internal finite state machine in
            // ServerLoginPacketListenerImpl can continue
            this.server.getPlayerList().remove(player);

            LogoutRules.DISCONNECTED_PLAYERS.remove(player);
        }
    }
}
