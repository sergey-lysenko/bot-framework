package works.lysenko.util.data.range;

import org.apache.commons.math3.fraction.Fraction;
import works.lysenko.util.apis.grid.v._ValuedRangeResult;
import works.lysenko.util.data.range.graph.Writer;
import works.lysenko.util.func.grid.Renderers;
import works.lysenko.util.grid.expected.ColoursQuotas;
import works.lysenko.util.grid.expected.QuotasHSB;
import works.lysenko.util.grid.record.graph.Options;

import java.util.Map;

import static java.util.Objects.isNull;
import static works.lysenko.Base.logTrace;
import static works.lysenko.util.data.records.KeyValue.kv;
import static works.lysenko.util.data.strs.Bind.b;
import static works.lysenko.util.data.strs.Case.c;
import static works.lysenko.util.data.strs.Vars.a;
import static works.lysenko.util.func.core.Assertions.fail;
import static works.lysenko.util.func.type.Objects.isNotNull;
import static works.lysenko.util.func.type.fractions.Render.ts;
import static works.lysenko.util.lang.D.DUE_TO;
import static works.lysenko.util.lang.U.UNABLE_TO;
import static works.lysenko.util.lang.word.A.ACTUAL;
import static works.lysenko.util.lang.word.C.CONFIGURATION;
import static works.lysenko.util.lang.word.E.EDGE;
import static works.lysenko.util.lang.word.E.EXPECTED;
import static works.lysenko.util.lang.word.G.GRAPH;
import static works.lysenko.util.lang.word.R.RENDER;
import static works.lysenko.util.lang.word.W.WRONG;

/**
 * Represents a Graph object that can render data points and rows based on different configurations.
 * The Graph class includes methods for creating different types of graphs and rendering them.
 *
 * @param <T> type of value
 * @param amount
 * @param fences
 * @param go
 * @param quotas
 * @param renderers
 * @param results
 * @param title
 */
@SuppressWarnings("unchecked")
public record Graph<T>(String title, IntegerRange amount, AbstractQuotas quotas, Map<T, _ValuedRangeResult> results,
                       Options go,
                       Renderers renderers,
                       Integer fences) {

    /**
     * Creates a new Graph object with the specified parameters.
     *
     * @param results   The Map containing data points for the graph.
     * @param go        Options object containing configuration options for the graph.
     * @param renderers Renderers object containing functions for data handling.
     * @param fences    The number of fences in the graph.
     * @param <T>       The type of value.
     * @return A1 new Graph object initialised with the provided parameters.
     */
    public static <T> Graph<T> graphActual(final Map<T, _ValuedRangeResult> results, final Options go,
                                           final Renderers renderers,
                                           final Integer fences) {

        return new Graph<>(c(ACTUAL), null, null, results, go, renderers, fences);
    }

    /**
     * Creates a new Graph object with the specified parameters.
     *
     * @param amount        The integer range defining the amount for the graph.
     * @param quotasColours The ColoursQuotas object containing share values.
     * @param go            Options object containing configuration options for the graph.
     * @param renderers     Renderers object containing functions for data handling.
     * @param <T>           The type of value.
     * @return A1 new Graph object initialised with the provided parameters.
     */
    public static <T> Graph<T> graphExpected(final IntegerRange amount, final ColoursQuotas quotasColours, final Options go,
                                             final Renderers renderers) {

        return new Graph<>(c(EXPECTED), amount, quotasColours, null, go, renderers, null);
    }

    /**
     * Creates a new Graph object with the specified parameters.
     *
     * @param amount    The integer range defining the amount for the graph.
     * @param QuotasHSB The QuotasHSB object containing share values.
     * @param go        Options object containing configuration options for the graph.
     * @param renderers Renderers object containing functions for data handling.
     * @param fences    The number of fences in the graph.
     * @param <T>       The type of value.
     * @return A1 new Graph object initialised with the provided parameters.
     */
    public static <T> Graph<T> graphExpected(final IntegerRange amount, final QuotasHSB QuotasHSB, final Options go,
                                             final Renderers renderers, final int fences) {

        return new Graph<>(c(EXPECTED), amount, QuotasHSB, null, go, renderers, fences);
    }

    /**
     * Renders a graphical representation of data and returns the resulting fraction value.
     * The rendering process is determined by the configuration of the quotas and results properties.
     * Depending on these configurations, it generates either an expected or an actual graph.
     * Utilizes the Writer class for the graph rendering process.
     * Logs debug information and throws an exception if unable to render due to incorrect configuration.
     *
     * @return Fraction representing the edge value generated by the rendering process.
     * @throws IllegalStateException if unable to render the graph due to an incorrect configuration.
     */
    @SuppressWarnings({"PublicMethodNotExposedInInterface", "ChainOfInstanceofChecks", "unchecked"})
    public Fraction render() {

        Fraction edge = null;
        if (isNotNull(quotas) && isNull(results)) { // Expected
            if (quotas instanceof ColoursQuotas)
                edge = Writer.graphExpectedInteger(title, amount, quotas, go, renderers);
            if (quotas instanceof QuotasHSB)
                edge = Writer.graphExpectedFraction(title, amount, quotas, go, renderers, fences);
        }
        if (isNotNull(results) && isNull(quotas)) { // Actual
            final Map.Entry<?, _ValuedRangeResult> entry = results.entrySet().stream().findFirst().orElse(null);
            if (isNotNull(entry) && (entry.getKey() instanceof Integer))
                edge = Writer.graphActualInteger(title, (Map<Integer, _ValuedRangeResult>) results, go, renderers);
            if (isNotNull(entry) && (entry.getKey() instanceof Fraction))
                edge = Writer.graphActualFraction(title, (Map<Fraction, _ValuedRangeResult>) results, go, renderers, fences);
        }
        logTrace(a(kv(EDGE, ts(true, edge))));
        if (isNull(edge)) fail(b(UNABLE_TO, RENDER, GRAPH, DUE_TO, WRONG, CONFIGURATION));
        return edge;
    }

}