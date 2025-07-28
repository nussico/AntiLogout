package org.samo_lego.antilogout.mixin;

import java.util.List;

import org.samo_lego.antilogout.datatracker.LogoutRules;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.network.ClientConnection;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ConnectedClientData;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * Kicks same players that are in {@link LogoutRules#DISCONNECTED_PLAYERS} list
 * when player with same UUID joins.
 */
@Mixin(PlayerManager.class)
public abstract class MixinPlayerList {

    @Shadow
    @Final
    private MinecraftServer server;

    @Shadow
    public abstract List<ServerPlayerEntity> getPlayerList();

    /**
     * When a player wants to connect but is still online,
     * we allow players with same uuid to be disconnected.
     */
    @Inject(method = "onPlayerConnect(Lnet/minecraft/network/ClientConnection;Lnet/minecraft/server/network/ServerPlayerEntity;Lnet/minecraft/server/network/ConnectedClientData;)V", at = @At("HEAD"))
    private void onPlayerConnect(ClientConnection clientConnection, ServerPlayerEntity serverPlayerEntity,
            ConnectedClientData connectedClientData,
            CallbackInfo ci) {
        var matchingPlayers = getPlayerList().stream()
                .filter(player -> player.getUuid().equals(serverPlayerEntity.getUuid()))
                .toList();

        for (ServerPlayerEntity player : matchingPlayers) {
            // Allows disconnect
            ((LogoutRules) player).al_setAllowDisconnect(true);

            // Removes player so that the internal finite state machine in
            // ServerLoginPacketListenerImpl can continue
            this.server.getPlayerManager().remove(player);

            LogoutRules.DISCONNECTED_PLAYERS.remove(player);
        }
    }
}
