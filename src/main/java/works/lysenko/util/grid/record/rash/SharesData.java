package works.lysenko.util.grid.record.rash;

/**
 * Class representing data for shares including the value, i, maximum value, and width.
 *
 * @param <T> the type of the value
 */
public record SharesData<T>(T value, int i, Double max, int width) {}
