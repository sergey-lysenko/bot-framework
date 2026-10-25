package works.lysenko.util.data.enums;

import works.lysenko.util.apis.data._Marker;

import static works.lysenko.util.chrs.__.TO;
import static works.lysenko.util.chrs.____.FROM;

/**
 * Enumeration representing delimiters/markers for text parsing or ranges.
 */
@SuppressWarnings("Singleton")
public enum Marker implements _Marker {

    /**
     * Marker pair from "FROM" to "TO".
     */
    FROM_TO(FROM, TO);

    private final String first;
    private final String second;

    Marker(final String first, final String second) {

        this.first = first;
        this.second = second;
    }

    /**
     * @return the first marker string
     */
    public String getFirst() {

        return first;
    }

    /**
     * @return the second marker string
     */
    public String getSecond() {

        return second;
    }
}
