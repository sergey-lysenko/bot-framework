package works.lysenko.util.apis.grid.r;

import works.lysenko.util.data.range.IntegerRange;

/**
 * Interface representing a palette, typically managing a range of integer values.
 * Provides a method to retrieve the integer range associated with the palette.
 */
@SuppressWarnings("InterfaceWithOnlyOneDirectInheritor")
public interface _Palette {

    /**
     * Retrieves the integer range for the associated object.
     *
     * @return an IntegerRange object representing the range of integer values.
     */
    IntegerRange get();
}
