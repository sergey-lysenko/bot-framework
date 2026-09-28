package works.lysenko.util.prop.data;

import static works.lysenko.util.spec.PropEnum._DELIMITER_L0;
import static works.lysenko.util.spec.PropEnum._DELIMITER_L1;
import static works.lysenko.util.spec.PropEnum._DELIMITER_L2;

/**
 * Represents a class for defining delimiters used in text processing.
 */
public record Delimeters() {

    /**
     * Represents the first level delimiter character used in text processing.
     */
    public static final char L0 = ((CharSequence) _DELIMITER_L0.get()).charAt(0);
    /**
     * Represents the second-level delimiter character used in text processing.
     */
    public static final char L1 = ((CharSequence) _DELIMITER_L1.get()).charAt(0);
    /**
     * Represents the third-level delimiter character used in text processing.
     */
    public static final char L2 = ((CharSequence) _DELIMITER_L2.get()).charAt(0);
}
