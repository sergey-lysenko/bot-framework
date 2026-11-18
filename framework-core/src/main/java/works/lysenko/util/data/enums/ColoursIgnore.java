package works.lysenko.util.data.enums;

import works.lysenko.util.lang.word.O;
import works.lysenko.util.lang.word.S;
import works.lysenko.util.lang.word.U;

import java.util.HashSet;
import java.util.Set;

import static works.lysenko.util.func.type.Objects.isNotNull;

/**
 * The ColoursIgnore enum represents a set of constants used to define specific marker values.
 */
public enum ColoursIgnore {

    /**
     * Marker for ORDER ignored colours.
     */
    ORDER(O.ORDER),

    /**
     * Marker for OTHER ignored colours.
     */
    OTHER(O.OTHER),

    /**
     * Marker for SHRINK ignored colours.
     */
    SHRINK(S.SHRINK),

    /**
     * Marker for UPDATE ignored colours.
     */
    UPDATE(U.UPDATE);

    private final String marker;

    ColoursIgnore(final String marker) {

        this.marker = marker;
    }

    /**
     * Constructs a set of ColoursIgnore enums based on the provided markers string.
     *
     * @param markers A1 string containing markers to check against.
     * @return A1 set of ColoursIgnore enums where the marker is present in the provided markers string.
     */
    @SuppressWarnings("SetReplaceableByEnumSet")
    public static Set<ColoursIgnore> set(final String markers) {

        final Set<ColoursIgnore> set = new HashSet<>(0);
        if (isNotNull(markers))
            for (final ColoursIgnore ignore : values())
                if (markers.contains(ignore.marker()))
                    set.add(ignore);
        return set;
    }

    /**
     * Retrieves the marker value associated with the ColoursIgnore enum.
     *
     * @return the marker value as a String
     */
    @SuppressWarnings("PublicMethodNotExposedInInterface")
    public String marker() {

        return marker;
    }
}
