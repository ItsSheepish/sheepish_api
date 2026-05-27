package its.sheepish.sheepish_api.misc;

/**
 * Utility class for converting time values between seconds and Minecraft ticks.
 * <p>
 * Standard Minecraft logic assumes a constant target rate of 20 game ticks per second.
 */
public final class SecondsToTicks {

    public static final int TICKS_PER_SECOND = 20;
    public static final double SECONDS_PER_TICK = 1.0 / TICKS_PER_SECOND; // 0.05 seconds

    // Private constructor to prevent instantiation of a pure utility class
    private SecondsToTicks() {}

    /**
     * Converts a whole number of seconds into game ticks.
     *
     * @param seconds The amount of seconds.
     * @return The equivalent duration in Minecraft ticks.
     */
    public static int toTicks(int seconds) {
        return seconds * TICKS_PER_SECOND;
    }

    /**
     * Converts a fractional/precise number of seconds into game ticks.
     * Automatically rounds to the nearest whole tick.
     *
     * @param seconds The precise amount of seconds (e.g., 1.5 or 0.25).
     * @return The closest equivalent duration in Minecraft ticks.
     */
    public static int toTicks(double seconds) {
        return (int) Math.round(seconds * TICKS_PER_SECOND);
    }

    /**
     * Converts Minecraft ticks back into a readable decimal number of seconds.
     *
     * @param ticks The amount of game ticks.
     * @return The equivalent duration in seconds.
     */
    public static double toSeconds(int ticks) {
        return ticks * SECONDS_PER_TICK;
    }
}