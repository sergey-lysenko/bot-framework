package works.lysenko.util.data.type;

/**
 * Represents a value with associated precision and a timestamp.
 * The class is immutable and uses a record structure.
 *
 * @param value    The main numeric value. It may be null to indicate the absence of a value.
 * @param precision The precision of the numeric value. It may be null to indicate unspecified precision.
 * @param stamp    A timestamp or identifier associated with the value. It may be null to indicate unspecified or missing timestamp.
 */
public record Value(Integer value, Integer precision, String stamp) {}
