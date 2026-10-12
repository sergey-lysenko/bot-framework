package works.lysenko.util.prop.data;

import static works.lysenko.util.spec.PropEnum._DATA_DELIMITER_L0;
import static works.lysenko.util.spec.PropEnum._DATA_DELIMITER_L1;
import static works.lysenko.util.spec.PropEnum._DATA_DELIMITER_L2;

/**
 * Represents a class for defining delimiters used in text processing.
 */
public record Delimeters() {

    private static char getChar(final works.lysenko.util.apis._PropEnum p, final char fallback) {
        try {
            if (null != works.lysenko.Base.properties) {
                final Object val = works.lysenko.Base.properties.getEnum(p);
                if (val instanceof CharSequence cs && cs.length() > 0) return cs.charAt(0);
            }
        } catch (final Exception ignored) {}
        return fallback;
    }

    /**
     * Represents the first level delimiter character used in text processing.
     */
    public static final char L0 = getChar(_DATA_DELIMITER_L0, '|');
    /**
     * Represents the second-level delimiter character used in text processing.
     */
    public static final char L1 = getChar(_DATA_DELIMITER_L1, ',');
    /**
     * Represents the third-level delimiter character used in text processing.
     */
    public static final char L2 = getChar(_DATA_DELIMITER_L2, ';');
}
