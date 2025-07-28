package org.samo_lego.antilogout.datatracker;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.jetbrains.annotations.ApiStatus;
import org.samo_lego.antilogout.AntiLogout;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public interface LogoutRules {
    /**
     * Marks this disconnect as AFK-triggered.
     */
    void al_setAfkDisconnect(boolean afk);

    /**
     * Returns true if the disconnect was triggered by AFK command.
     */
    boolean al_isAfkDisconnect();

    /**
     * Set of players that have disconnected,
     * but are still present in the world.
     */
    Set<ServerPlayerEntity> DISCONNECTED_PLAYERS = new HashSet<>();

    Map<UUID, Text> SKIPPED_DEATH_MESSAGES = new HashMap<>();

    /**
     * Whether to allow disconnect for this player.
     *
     * @return true if allowed, false otherwise
     */
    boolean al_allowDisconnect();

    /**
     * Sets the time when the player can disconnect.
     *
     * @param systemTime time in milliseconds at which the player can disconnect
     *                   without staying in the world.
     */
    void al_setAllowDisconnectAt(long systemTime);

    /**
     * Sets whether the player can disconnect.
     *
     * @param allow true if disconnect is allowed, false otherwise
     */

    void al_setAllowDisconnect(boolean allow);

    /**
     * Marks the player as in combat state until the specified time.
     *
     * @param systemTime time in milliseconds at which the player leaves state.
     */
    default void al_setInCombatUntil(long systemTime) {
        this.al_setAllowDisconnectAt(systemTime);

        if (AntiLogout.config.combatLog.notifyOnCombat) {
            // Inform player
            long duration = (long) Math.ceil((systemTime - System.currentTimeMillis()) / 1000.0D);
            ((ServerPlayerEntity) this).sendMessage(this.al$getStartCombatMessage(duration), true);

            this.al$delay(systemTime,
                    () -> ((ServerPlayerEntity) this).sendMessage(this.al$getEndCombatMessage(duration), true));
        }
    }

    /**
     * Schedules a task execution after the specified delay.
     * Only one can be scheduled at a time.
     * (Scheduling new task will cancel the previous one)
     *
     * @param at   system time at which the task should be executed
     * @param task task to execute
     */
    void al$delay(long at, Runnable task);

    /**
     * Gets the combat message.
     *
     * @param duration duration of combat state in seconds
     * @return combat message
     */
    @ApiStatus.Internal
    default Text al$getStartCombatMessage(long duration) {
        return Text.literal("[AL] ").formatted(Formatting.DARK_RED).append(
                Text.translatable(AntiLogout.config.combatLog.combatEnterMessage, duration)
                        .formatted(Formatting.RED));
    }

    @ApiStatus.Internal
    default Text al$getEndCombatMessage(long duration) {
        return Text.literal("[AL] ").formatted(Formatting.DARK_GREEN).append(
                Text.translatable(AntiLogout.config.combatLog.combatEndMessage, duration)
                        .formatted(Formatting.GREEN));
    }

    /**
     * Whether the player is fake (present in the world, but not connected).
     *
     * @return true if fake, false otherwise
     */
    boolean al_isFake();

    /**
     * Called when the player disconnects.
     */
    void al_onRealDisconnect();

    /**
     * Gets the current system time (ms) at which the player is allowed to disconnect.
     * Used for updating timers on config reload.
     */
    long al_getAllowDisconnectTime();
}
