package works.lysenko.util.data.records.test;

/**
 * Represents a Triplet of Throwables.
 * <p>
 * A Triplet is an immutable record class that holds three Throwables: ultimate, penultimate, and antepenultimate.
 *
 * @param ultimate         the most recent (ultimate) Throwable
 * @param penultimate      the second most recent (penultimate) Throwable
 * @param antepenultimate the third most recent (antepenultimate) Throwable
 */
public record Triplet(Throwable ultimate, Throwable penultimate, Throwable antepenultimate) {}
