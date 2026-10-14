package works.lysenko.util.prop.data;

import static works.lysenko.util.prop.PropEnum._DATA_DELIMITER_L0;
import static works.lysenko.util.prop.PropEnum._DATA_DELIMITER_L1;
import static works.lysenko.util.prop.PropEnum._DATA_DELIMITER_L2;

/**
 * Defines hierarchical delimiter characters used for data parsing and text formatting.
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
     * Level 0 (top-level) delimiter character used in data parsing and text formatting.
     */
    public static final char L0 = getChar(_DATA_DELIMITER_L0, '|');

    /**
     * Level 1 (secondary) delimiter character used in data parsing and text formatting.
     */
    public static final char L1 = getChar(_DATA_DELIMITER_L1, ',');

    /**
     * Level 2 (tertiary) delimiter character used in data parsing and text formatting.
     */
    public static final char L2 = getChar(_DATA_DELIMITER_L2, ';');
}
