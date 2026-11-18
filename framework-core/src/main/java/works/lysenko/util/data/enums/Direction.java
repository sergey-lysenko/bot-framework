package works.lysenko.util.data.enums;

/**
 * The Direction enum represents two possible directions with associated integer values.
 */
public enum Direction {

    /**
     * Negative direction (-1).
     */
    NEGATIVE(-1),

    /**
     * Positive direction (1).
     */
    POSITIVE(1);

    private final int value;

    Direction(final int value) {

        this.value = value;
    }

    /**
     * Retrieves the integer value associated with the direction.
     *
     * @return The integer value representing the direction.
     */
    @SuppressWarnings("PublicMethodNotExposedInInterface")
    public int value() {

        return value;
    }
}
