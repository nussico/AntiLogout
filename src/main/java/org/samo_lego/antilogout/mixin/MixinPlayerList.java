package org.samo_lego.antilogout.mixin;

import net.minecraft.network.Connection;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.level.ServerPlayer;
import org.samo_lego.antilogout.datatracker.LogoutRules;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * Mixin for PlayerManager to handle duplicate player connections.
 * Kicks players that are in {@link LogoutRules#DISCONNECTED_PLAYERS} when a player with the same UUID joins.
 */
@Mixin(PlayerList.class)
public abstract class MixinPlayerList {

    @Shadow
    @Final
    private MinecraftServer server;

    @Shadow
    public abstract List<ServerPlayer> getPlayers();

    /**
     * Handles player connection when a player with the same UUID is already online.
     * Allows the old player to disconnect and removes them from the world and dummy list.
     * @param clientConnection the connecting player's network connection
     * @param serverPlayerEntity the connecting player entity
     * @param connectedClientData client data
     * @param ci callback info
     */
        @Inject(method = "placeNewPlayer", at = @At("HEAD"))
        private void onPlayerConnect(Connection clientConnection, ServerPlayer serverPlayerEntity,
            CommonListenerCookie connectedClientData,
            CallbackInfo ci) {
        var matchingPlayers = getPlayers().stream()
            .filter(player -> player.getUUID().equals(serverPlayerEntity.getUUID()))
                .toList();

        for (ServerPlayer player : matchingPlayers) {
            // Allow disconnect for the old player
            ((LogoutRules) player).al_setAllowDisconnect(true);

            // Remove the old player so the login process can continue
            this.server.getPlayerList().remove(player);

            // Remove from dummy/disconnected list
            LogoutRules.DISCONNECTED_PLAYERS.remove(player);
        }
    }
}
