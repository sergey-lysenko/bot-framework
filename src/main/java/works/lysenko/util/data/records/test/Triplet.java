package works.lysenko.util.data.records.test;

/**
 * Represents a Triplet of Throwables.
 * <p>
 * A1 Triplet is an immutable record class that holds three Throwables: ultimate, penultimate, and antepenultimate.
 */
public record Triplet(Throwable ultimate, Throwable penultimate, Throwable antepenultimate) {}
