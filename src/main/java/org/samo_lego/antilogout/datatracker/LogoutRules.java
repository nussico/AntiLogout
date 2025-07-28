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
     * Sets whether this disconnect was triggered by the AFK command.
     * @param afk true if disconnect is AFK-triggered, false otherwise
     */
    void al_setAfkDisconnect(boolean afk);

    /**
     * Checks if the disconnect was triggered by the AFK command.
     * @return true if AFK disconnect, false otherwise
     */
    boolean al_isAfkDisconnect();

    /**
     * Set of players that have disconnected but are still present in the world as dummies.
     */
    Set<ServerPlayerEntity> DISCONNECTED_PLAYERS = new HashSet<>();

    Map<UUID, Text> SKIPPED_DEATH_MESSAGES = new HashMap<>();

    /**
     * Checks whether the player is currently allowed to disconnect without leaving a dummy.
     * @return true if allowed, false otherwise
     */
    boolean al_allowDisconnect();

    /**
     * Sets the system time (in ms) when the player is allowed to disconnect without leaving a dummy.
     * @param systemTime time in milliseconds when disconnect is allowed
     */
    void al_setAllowDisconnectAt(long systemTime);

    /**
     * Sets whether the player can disconnect immediately.
     * @param allow true to allow immediate disconnect, false otherwise
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
            // Notify player about entering combat
            long duration = (long) Math.ceil((systemTime - System.currentTimeMillis()) / 1000.0D);
            ((ServerPlayerEntity) this).sendMessage(this.al$getStartCombatMessage(duration), true);

            this.al$delay(systemTime,
                    () -> ((ServerPlayerEntity) this).sendMessage(this.al$getEndCombatMessage(duration), true));
        }
    }

    /**
     * Schedules a task to be executed after the specified system time (in ms).
     * Only one task can be scheduled at a time; scheduling a new task cancels the previous one.
     * @param at system time in ms when the task should be executed
     * @param task the task to execute
     */
    void al$delay(long at, Runnable task);

    /**
     * Returns the combat start message for the player.
     * @param duration duration of combat state in seconds
     * @return the combat start message
     */
    @ApiStatus.Internal
    default Text al$getStartCombatMessage(long duration) {
        return Text.literal("[AL] ").formatted(Formatting.DARK_RED).append(
                Text.translatable(AntiLogout.config.combatLog.combatEnterMessage, duration)
                        .formatted(Formatting.RED));
    }

    /**
     * Returns the combat end message for the player.
     * @param duration duration of combat state in seconds
     * @return the combat end message
     */
    @ApiStatus.Internal
    default Text al$getEndCombatMessage(long duration) {
        return Text.literal("[AL] ").formatted(Formatting.DARK_GREEN).append(
                Text.translatable(AntiLogout.config.combatLog.combatEndMessage, duration)
                        .formatted(Formatting.GREEN));
    }

    /**
     * Checks whether the player is a fake/disconnected entity (present in the world, but not connected).
     * @return true if fake, false otherwise
     */
    boolean al_isFake();

    /**
     * Called when the player disconnects for real (not a soft/fake disconnect).
     */
    void al_onRealDisconnect();

    /**
     * Gets the current system time (ms) at which the player is allowed to disconnect.
     * Used for updating timers on config reload.
     * @return system time in ms when disconnect is allowed
     */
    long al_getAllowDisconnectTime();
}
