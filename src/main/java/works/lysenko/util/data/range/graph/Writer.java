package works.lysenko.util.data.range.graph;

import org.apache.commons.math3.fraction.Fraction;
import works.lysenko.util.apis.grid.q._Quotas;
import works.lysenko.util.apis.grid.v._ValuedRangeResult;
import works.lysenko.util.data.range.IntegerRange;
import works.lysenko.util.data.range.graph.values.OfFractions;
import works.lysenko.util.data.range.graph.values.OfIntegers;
import works.lysenko.util.func.grid.Renderers;
import works.lysenko.util.grid.record.graph.Options;

import java.util.Map;

import static works.lysenko.Base.logEmptyLine;

/**
 * Represents a class for logging data points and data rows of a graph.
 */
@SuppressWarnings({"StaticMethodOnlyUsedInOneClass", "TypeMayBeWeakened"})
public record Writer() {

    /**
     * Generates and returns a graph's computed edge fraction by processing fractional values
     * from the provided results, scaling them, and calculating relevant statistics.
     *
     * @param title     The title of the graph to be generated, used for identification purposes.
     * @param results   A map where each key is a Fraction and each value is a ValuedRangeResult
     *                  corresponding to that Fraction, used for calculations in the graph.
     * @param go        An Options object that configures the graph's attributes, such as width and
     *                  fractional values, influencing the graph's representation.
     * @param renderers A Renderers object that defines rendering functions responsible for handling
     *                  the data visualization or logging for the graph.
     * @param fences    An integer specifying the level of granularity or quantization for rendering.
     * @return A Fraction object representing the computed edge value of the graph after processing,
     * scaling, and statistical analysis.
     */
    public static Fraction graphActualFraction(final String title, final Map<Fraction, _ValuedRangeResult> results,
                                               final Options go, final Renderers renderers, final int fences) {

        final Fraction edge = OfFractions.actualValuesFromMapFraction(title, results, go, renderers, fences);
        Scale.scale(go, edge);
        Stats.stats(results);
        logEmptyLine();
        return edge;
    }

    /**
     * Generates and returns a computed edge fraction for a graph based on integer values
     * from the provided results, scales the graph, and processes statistical analysis.
     *
     * @param title     The title of the graph for identification purposes.
     * @param results   A map where keys are integers and values are ValuedRangeResult objects
     *                  associated with those integers, used for the graph computations.
     * @param go        An Options object configuring the graph's attributes, including width,
     *                  requested fraction, actual fraction, and edge details.
     * @param renderers A Renderers object defining rendering logic for data visualization.
     * @return A Fraction object representing the computed edge value of the graph after processing.
     */
    public static Fraction graphActualInteger(final String title, final Map<Integer, _ValuedRangeResult> results,
                                              final Options go, final Renderers renderers) {

        final Fraction edge = OfIntegers.actualValuesFromMapInteger(title, results, go, renderers);
        Scale.scale(go, edge);
        Stats.stats(results);
        logEmptyLine();
        return edge;
    }

    /**
     * Computes the expected fractional edge value for a graph based on integer shares, scales the graph,
     * and calculates corresponding statistics.
     *
     * @param title     The title of the graph to be generated, used for identification and descriptive purposes.
     * @param amount    An IntegerRange object that defines the scope of integers used within the computation.
     * @param quotas    A _Quotas collection containing fractional share values used for the calculations.
     * @param go        An Options object that configures various attributes of the graph during the computation process.
     * @param renderers A Renderers object responsible for defining the rendering or visualization of the graph.
     * @param fences    An integer representing the level of granularity or segmentation applied when rendering the graph.
     * @return A Fraction object representing the computed expected edge value of the graph after processing, scaling,
     * and statistical analysis.
     */
    public static Fraction graphExpectedFraction(final String title, final IntegerRange amount, final _Quotas<?
            extends Fraction> quotas, final Options go, final Renderers renderers, final int fences) {

        final Fraction edge = OfFractions.expectedValuesFromSharesFraction(title, amount, quotas, go, renderers, fences);
        Scale.scale(go, edge);
        Stats.stats(quotas);
        logEmptyLine();
        return edge;
    }

    /**
     * Computes the expected integer edge value for a graph based on integer quotas, scales the graph,
     * and performs statistical processing.
     *
     * @param title     The title of the graph to be generated, used for identification or descriptive purposes.
     * @param amount    An IntegerRange object that specifies the range of integers used in the computation.
     * @param quotas    A _Quotas<> object that contains the integer share values for the graph calculation.
     * @param go        An Options object that configures various attributes of the graph during processing.
     * @param renderers A Renderers object that defines the rendering or visualization logic for the graph.
     * @return A Fraction object representing the computed expected edge value of the graph after processing and scaling.
     */
    public static Fraction graphExpectedInteger(final String title, final IntegerRange amount, final _Quotas<Integer> quotas
            , final Options go, final Renderers renderers) {

        final Fraction edge = OfIntegers.expectedValuesFromSharesInteger(title, amount, quotas, go, renderers);
        Scale.scale(go, edge);
        Stats.stats(quotas);
        logEmptyLine();
        return edge;
    }
}