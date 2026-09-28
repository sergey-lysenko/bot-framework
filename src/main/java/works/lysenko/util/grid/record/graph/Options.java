package works.lysenko.util.grid.record.graph;

import org.apache.commons.math3.fraction.Fraction;
import works.lysenko.util.data.records.Slack;

/**
 * Represents options for configuring a graph, including width, and both requested and actual fractions.
 */
public record Options(int width, Fraction requested, Fraction actual, Slack slack, Fraction edge) {

    /**
     * Constructs a new `Options` object using the provided parameters.
     *
     * @param width     the width value for the graph configuration
     * @param requested the requested fraction value for the graph configuration
     * @param actual    the actual fraction value for the graph configuration
     * @param slack     the slack object representing allowed deviation and border in the configuration
     * @param edge      the edge fraction value for the graph configuration
     * @return a new `Options` instance initialized with the specified parameters
     */
    public static Options go(final int width, final Fraction requested, final Fraction actual, final Slack slack,
                             final Fraction edge) {

        return new Options(width, requested, actual, slack, edge);
    }

}
