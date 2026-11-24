package works.lysenko.util.data.type;

/**
 * Represents a value with associated precision, timestamp, and optional symbolic colour token.
 * The class is immutable and uses a record structure.
 *
 * @param value     the main numeric value. It may be null to indicate the absence of a value.
 * @param precision the precision of the numeric value. It may be null to indicate unspecified precision.
 * @param stamp     a timestamp or identifier associated with the value. It may be null to indicate unspecified or missing timestamp.
 * @param symbol    an optional symbolic colour token (e.g. {@code SURFACE_BG}) resolved via a theme or known colours; null for literal integers.
 */
public record Value(Integer value, Integer precision, String stamp, String symbol) {

    /**
     * Constructs a Value without a symbolic colour token.
     *
     * @param value     the main numeric value
     * @param precision the precision of the numeric value
     * @param stamp     a timestamp or identifier associated with the value
     */
    public Value(final Integer value, final Integer precision, final String stamp) {

        this(value, precision, stamp, null);
    }
}
