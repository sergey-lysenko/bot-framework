package works.lysenko.util.func.grid.ranger;

import org.apache.commons.math3.fraction.Fraction;
import works.lysenko.util.apis.grid.q._Quotas;
import works.lysenko.util.data.range.Graph;
import works.lysenko.util.func.grid.Renderers;
import works.lysenko.util.apis.grid.v._ValuedRangeResult;
import works.lysenko.util.grid.record.graph.Options;
import works.lysenko.util.grid.record.meta.ValidationMeta;
import works.lysenko.util.prop.grid.Ranges;

import java.util.Map;

import static works.lysenko.Base.log;
import static works.lysenko.util.data.range.graph.Calculator.maxMapValue;
import static works.lysenko.util.data.range.graph.Calculator.maxShare;
import static works.lysenko.util.data.strs.Bind.b;
import static works.lysenko.util.data.strs.Case.c;
import static works.lysenko.util.data.strs.Swap.qinn;
import static works.lysenko.util.grid.record.graph.Options.go;
import static works.lysenko.util.lang.word.D.DISTRIBUTION;
import static works.lysenko.util.lang.word.V.VERIFYING;

/**
 * The Render class handles rendering operations for specific validation and share data.
 *
 * @param <T> The type parameter representing the data type used in this class.
 */
@SuppressWarnings("PublicMethodNotExposedInInterface")
public class Render<T> {

    private final ValidationMeta meta;
    private final _Quotas<T> expected;
    private final Renderers renderers;
    private final Map<T, _ValuedRangeResult> actual;

    /**
     * Constructs a Render instance for handling rendering operations with the given validation meta,
     * expected shares, actual shares, and renderers.
     *
     * @param meta      The meta-information for the validation request.
     * @param expected  The expected shares' data.
     * @param actual    The actual shares data represented as a map.
     * @param renderers The rendering functions to be used.
     */
    public Render(final ValidationMeta meta, final _Quotas<T> expected, final Map<T, _ValuedRangeResult> actual,
                  final Renderers renderers) {

        this.meta = meta;
        this.expected = expected;
        this.actual = actual;
        this.renderers = renderers;
    }

    /**
     * Executes the rendering process for actual data using the specified edge fraction.
     * It prepares the options and setups the graph for rendering using the given meta-information
     * and renderers associated with the instance.
     *
     * @param edge The fraction value to be used as the edge parameter in the rendering options.
     */
    public final void actual(final Fraction edge) {

        final Options go = go(Ranges.width, maxShare(expected.get()), maxMapValue(actual), null, edge);
        Graph.graphActual(actual, go, renderers, meta.fences()).render();
    }

    /**
     * Executes the expected rendering process by logging relevant information
     * and returns a Fraction object representing the result of this process.
     *
     * @return A1 Fraction object that represents the calculated value from the logging process.
     */
    public final Fraction expected() {

        return expected.logExpected(Ranges.width, renderers);
    }

    /**
     * Logs a message composed of various parts including verifying status,
     * meta-information name, subject's plural form, and a distribution keyword.
     */
    public final void title() {

        log(b(c(VERIFYING), qinn(meta.vr().name(), null), meta.subject().noun().plural(), DISTRIBUTION));
    }
}
