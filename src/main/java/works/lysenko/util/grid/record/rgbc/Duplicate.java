package works.lysenko.util.grid.record.rgbc;

import java.util.List;

/**
 * Represents a duplicate with an RGB value and a list of names.
 *
 * @param rgb   the RGB value of the duplicate
 * @param names the list of names associated with the duplicate
 */
record Duplicate(int rgb, List<String> names) {}
