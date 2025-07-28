package org.samo_lego.antilogout.mixin;

import net.minecraft.entity.damage.DamageSource;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.GameRules;
import org.samo_lego.antilogout.datatracker.LogoutRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayerEntity.class)
public abstract class MixinServerPlayerDeathMsgSaver {

    @Unique
    private static final int MAX_DEATH_MESSAGE_LENGTH = 256;
    @Unique
    private final ServerPlayerEntity self = (ServerPlayerEntity) (Object) this;

    @Unique
    public abstract net.minecraft.world.World level();

    /**
     * Injects into the player death handler to save death messages for fake/disconnected players.
     * Stores the message in SKIPPED_DEATH_MESSAGES for later display.
     * @param damageSource the source of damage
     * @param ci callback info
     */
    @Inject(method = "onDeath", at = @At("RETURN"))
    private void onDeath(DamageSource damageSource, CallbackInfo ci) {
        if (((LogoutRules) this).al_isFake()) {
            ServerWorld serverLevel = (ServerWorld) this.level();
            boolean seeDeathMsgs = serverLevel.getGameRules().getBoolean(GameRules.SHOW_DEATH_MESSAGES);

            Text deathMsg;
            if (seeDeathMsgs) {
                deathMsg = self.getDamageTracker().getDeathMessage();

                if (deathMsg.getString().length() > MAX_DEATH_MESSAGE_LENGTH) {
                    String string = deathMsg.asTruncatedString(MAX_DEATH_MESSAGE_LENGTH);
                    var attackTooLongMsg = Text.translatable("death.attack.message_too_long",
                            Text.literal(string).formatted(Formatting.YELLOW));

                    deathMsg = Text.translatable("death.attack.even_more_magic", self.getDisplayName())
                            .styled(style -> style.withHoverEvent(new HoverEvent.ShowText(attackTooLongMsg)));
                }
            } else {
                deathMsg = ScreenTexts.EMPTY;
            }
            LogoutRules.SKIPPED_DEATH_MESSAGES.put(self.getUuid(), deathMsg);
        }
    }
}
