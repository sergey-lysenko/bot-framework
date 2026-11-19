package works.lysenko.util.grid.record.rash;

/**
 * Class representing data for shares including the value, index, maximum value, and width.
 *
 * @param <T>   the type of the value
 * @param value the value associated with shares
 * @param i     the index value
 * @param max   the maximum value
 * @param width the width value
 */
public record SharesData<T>(T value, int i, Double max, int width) {}
