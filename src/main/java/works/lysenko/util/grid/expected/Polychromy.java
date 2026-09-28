package works.lysenko.util.grid.expected;

import works.lysenko.util.apis.grid.r._Polychromy;
import works.lysenko.util.data.enums.PolychromyMethod;
import works.lysenko.util.data.range.FractionRange;

import static java.util.Objects.isNull;
import static org.apache.commons.math3.fraction.Fraction.ONE;
import static org.apache.commons.math3.fraction.Fraction.ZERO;
import static works.lysenko.util.data.strs.Swap.i;

/**
 * Represents a Polychromy object that holds a FractionRange and a PolychromyMethod.
 */
@SuppressWarnings("DataFlowIssue")
public class Polychromy implements _Polychromy {

    private final PolychromyMethod method;
    private final boolean ignoreUpdate;
    private FractionRange range;

    /**
     * Constructs a new Polychromy object with the given source, method, and ignoreUpdate flag.
     *
     * @param source       the source string to extract values from
     * @param method       the method index to determine the PolychromyMethod
     * @param ignoreUpdate the flag to determine whether to ignore update or not
     */
    public Polychromy(final String source, final String method, final boolean ignoreUpdate) {

        this.ignoreUpdate = ignoreUpdate;

        PolychromyMethod candidate;
        if (isNull(source)) range = null;
        else range = new FractionRange(source, ZERO, ONE);
        if (isNull(method)) candidate = null;
        else
            try {
                candidate = PolychromyMethod.getByIndex(i(method));
            } catch (final NumberFormatException e) {
                candidate = null;
            }
        this.method = candidate;
    }

    public final FractionRange getRange() {

        return range;
    }

    @Override
    public final void setRange(final FractionRange range) {

        this.range = range;
    }

    public final boolean ignoreUpdate() {

        return ignoreUpdate;
    }

    public final PolychromyMethod method() {

        return method;
    }
}
