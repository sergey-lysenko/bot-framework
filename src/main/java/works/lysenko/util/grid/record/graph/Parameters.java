package works.lysenko.util.grid.record.graph;

import works.lysenko.util.data.records.Slack;

/**
 * Represents the parameters used to configure a graph.
 *
 * @param minimalMaximumValue The minimal maximum value for the graph.
 * @param amountS             The amount string used in the graph context.
 * @param slack               The slack factor as a Fraction object.
 */
public record Parameters(double minimalMaximumValue, String amountS, Slack slack) {}