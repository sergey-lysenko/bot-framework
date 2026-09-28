package works.lysenko.util.func.grid.polychromy;

import org.apache.commons.math3.fraction.Fraction;
import works.lysenko.util.apis.grid.g._GridProperties;
import works.lysenko.util.apis.grid.r._Polychromy;
import works.lysenko.util.apis.grid.r._Processor;
import works.lysenko.util.data.range.FractionRange;
import works.lysenko.util.func.logs.Trace;
import works.lysenko.util.grid.record.Request;
import works.lysenko.util.prop.grid.Allowed;
import works.lysenko.util.prop.grid.Update;

import static org.apache.commons.math3.util.FastMath.abs;
import static works.lysenko.Base.log;
import static works.lysenko.Base.logEvent;
import static works.lysenko.util.apis.grid.g._GridProperties.getGridPropertiesByName;
import static works.lysenko.util.apis.grid.g._GridProperties.putPropertiesToGridFile;
import static works.lysenko.util.data.enums.Ansi.yb;
import static works.lysenko.util.data.enums.Brackets.CURLY;
import static works.lysenko.util.data.enums.Severity.S0;
import static works.lysenko.util.data.strs.Bind.b;
import static works.lysenko.util.data.strs.Case.c;
import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.data.strs.Wrap.e;
import static works.lysenko.util.func.logs.trace.Data.traceable;
import static works.lysenko.util.func.type.Objects.isNotNull;
import static works.lysenko.util.lang.E.EXPECTATIONS_UPDATE_REQUESTED_BY___;
import static works.lysenko.util.lang.E.EXPECTATIONS_UPDATE_WAS_NOT___;
import static works.lysenko.util.lang.I.IS_BIGGER_THAN_ALLOWED;
import static works.lysenko.util.lang.N.NEW_COLOURS_PROPERTY___;
import static works.lysenko.util.lang.O.ORIGINAL_EXPECTATIONS_PERSISTED;
import static works.lysenko.util.lang.word.M.MARGIN;
import static works.lysenko.util.lang.word.P.POLYCHROMY;

/**
 * Represents a Processor object that processes negative results and updates expected property values.
 */
@SuppressWarnings({"CallToSuspiciousStringMethod", "MethodWithMultipleReturnPoints"})
public record Processor(Fraction actual, _Polychromy expected) implements _Processor {

    public void processNegativeResult(final Request vr) {

        final String original = expected.getRange().toString();
        expected.setRange(mergeFailedActualAndExpected());
        final String newValue = expected.getRange().toString();

        if (newValue.equals(original)) log(b(ORIGINAL_EXPECTATIONS_PERSISTED, yb(e(CURLY, newValue))));
        else log(b(NEW_COLOURS_PROPERTY___, yb(e(CURLY, newValue))));

        if (Update.polychromy) {
            if (vr.irq().polychromy().ignoreUpdate())
                log(b(c(POLYCHROMY), EXPECTATIONS_UPDATE_REQUESTED_BY___));
            else updateExpected(vr.name(), newValue);
        } else log(b(c(POLYCHROMY), EXPECTATIONS_UPDATE_WAS_NOT___));
    }

    @Override
    public void processPositiveResult(final Request vr) {

    }

    /**
     * Updates the expected polychromy value for a given grid.
     *
     * @param name             the path or name of the grid file
     * @param newPropertyValue the new value for the polychromy property
     */
    public void updateExpected(final String name, final String newPropertyValue) {

        final _GridProperties gridProperties = getGridPropertiesByName(name);
        gridProperties.updateProperty(POLYCHROMY, newPropertyValue);
        putPropertiesToGridFile(gridProperties, name);
    }

    /**
     * Merges the failed actual value with the expected range if the deviation is acceptable.
     * <p>
     * This method calculates the deviation between the expected range and the actual value.
     * It logs the relevant information and then checks if the deviation is within an acceptable range.
     * If the deviation is acceptable, it returns a new FractionRange combining the expected range and the actual value.
     * Otherwise, it logs an event and returns the expected range as it is.
     *
     * @return a new FractionRange combining the expected range and the actual value if the deviation is acceptable,
     * otherwise the original expected range
     */
    private FractionRange mergeFailedActualAndExpected() {

        final FractionRange expectedRange = expected.getRange();
        final Fraction margin = (isNotNull(expectedRange)) ? expectedRange.margin(actual) : null;
        Trace.log(traceable(actual, expectedRange, margin));

        if (isNotNull(margin)) {
            final boolean acceptable =
                    (isNotNull(Allowed.defaultPolychromyMargin) && Allowed.defaultPolychromyMargin.doubleValue() >= abs(margin.doubleValue()));
            if (acceptable) return new FractionRange(expected.getRange(), actual);
            else
                logEvent(S0, b(c(POLYCHROMY), MARGIN, s(margin.doubleValue()), IS_BIGGER_THAN_ALLOWED,
                        s(Allowed.defaultPolychromyMargin.doubleValue())));
        }
        return expectedRange;
    }
}
