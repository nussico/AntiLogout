package org.samo_lego.antilogout.mixin;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.world.level.gamerules.GameRules;
import org.samo_lego.antilogout.datatracker.LogoutRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayer.class)
public abstract class MixinServerPlayerDeathMsgSaver {

    @Unique
    private static final int MAX_DEATH_MESSAGE_LENGTH = 256;
    @Unique
    private final ServerPlayer self = (ServerPlayer) (Object) this;

    @Shadow
    public abstract ServerLevel level();

    /**
     * Injects into the player death handler to save death messages for fake/disconnected players.
     * Stores the message in SKIPPED_DEATH_MESSAGES for later display.
     * @param damageSource the source of damage
     * @param ci callback info
     */
    @Inject(method = "die", at = @At("RETURN"))
    private void onDeath(DamageSource damageSource, CallbackInfo ci) {
        if (((LogoutRules) this).al_isFake()) {
            ServerLevel serverLevel = (ServerLevel) this.level();
            boolean seeDeathMsgs = serverLevel.getGameRules().get(GameRules.SHOW_DEATH_MESSAGES);

            Component deathMsg;
            if (seeDeathMsgs) {
                deathMsg = self.getCombatTracker().getDeathMessage();

                if (deathMsg.getString().length() > MAX_DEATH_MESSAGE_LENGTH) {
                    String string = deathMsg.getString().substring(0, MAX_DEATH_MESSAGE_LENGTH);
                        var attackTooLongMsg = Component.translatable("death.attack.message_too_long",
                            Component.literal(string).withStyle(ChatFormatting.YELLOW));

                        deathMsg = Component.translatable("death.attack.even_more_magic", self.getDisplayName())
                            .withStyle(style -> style.withHoverEvent(new HoverEvent.ShowText(attackTooLongMsg)));
                }
            } else {
                deathMsg = CommonComponents.EMPTY;
            }
            LogoutRules.SKIPPED_DEATH_MESSAGES.put(self.getUUID(), deathMsg);
        }
    }
}
