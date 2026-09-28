package works.lysenko.util.func.grid.polychromy;

import org.apache.commons.math3.fraction.Fraction;
import works.lysenko.util.apis.data._RangedMargin;
import works.lysenko.util.apis.grid.g._Grid;
import works.lysenko.util.apis.grid.r._Polychromy;
import works.lysenko.util.data.enums.PolychromyMethod;
import works.lysenko.util.data.enums.RangeResult;
import works.lysenko.util.data.records.Noun;
import works.lysenko.util.data.records.RGB24;
import works.lysenko.util.data.records.diff.Pair;
import works.lysenko.util.grid.record.Request;
import works.lysenko.util.grid.record.meta.Method;
import works.lysenko.util.grid.record.meta.Subject;
import works.lysenko.util.grid.record.meta.ValidationMeta;
import works.lysenko.util.prop.grid.Allowed;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static java.util.Objects.isNull;
import static works.lysenko.Base.log;
import static works.lysenko.util.data.enums.RelativeOrAbsolute.ABSOLUTE;
import static works.lysenko.util.data.enums.Severity.S0;
import static works.lysenko.util.data.records.RangedMargin.rm;
import static works.lysenko.util.data.records.RelativeOrAbsoluteFraction.raf;
import static works.lysenko.util.data.strs.Bind.b;
import static works.lysenko.util.data.strs.Case.c;
import static works.lysenko.util.data.strs.Null.n;
import static works.lysenko.util.data.strs.Swap.v;
import static works.lysenko.util.data.strs.Wrap.q;
import static works.lysenko.util.func.core.Events.logPolychromyUndefinedBoundsFailure;
import static works.lysenko.util.func.core.Events.logPolychromyUndefinedMethodFailure;
import static works.lysenko.util.func.imgs.Analyser.calculatePolychromy;
import static works.lysenko.util.func.logs.OutOfBounds.logOutOfBounds;
import static works.lysenko.util.lang.word.C.CALCULATING;
import static works.lysenko.util.lang.word.I.IMAGE;
import static works.lysenko.util.lang.word.P.POLYCHROMY;

/**
 * The Routines class represents a utility for calculating and validating polychromy values.
 */
public record Routines() {

    /**
     * Calculates the actual polychromy value for a given name based on provided pixel data and method.
     * This method uses a specific polychromy calculation method to process colour data and derive the result.
     * If no method is specified, it logs a failure.
     *
     * @param name           the name or identifier for the process, used in logging
     * @param pixelsByColour a map where each key represents a colour (as an Integer) and the value is
     *                       a Pair containing the pixel count (left value) and additional information (right value)
     * @param method         the polychromy calculation method to use; if null, a failure is logged
     * @return a Fraction representing the calculated polychromy value
     */
    @SuppressWarnings("MethodWithMultipleReturnPoints")
    private static Fraction calculateActual(final String name, final Map<Integer, Pair<Integer, String>> pixelsByColour,
                                            final PolychromyMethod method) {

        log(b(c(CALCULATING), q(name), POLYCHROMY));
        final List<RGB24> points =
                pixelsByColour.entrySet().stream().flatMap(entry -> Collections.nCopies(entry.getValue().l(),
                        RGB24.rgb24(entry.getKey())).stream()).toList();
        if (isNull(method)) return logPolychromyUndefinedMethodFailure(name);
        return calculatePolychromy(points, method);
    }

    /**
     * Validates whether the actual polychromy value lies within the expected range defined in the provided request.
     * If the range is undefined or the value exceeds the allowable bounds, an appropriate result is logged and returned.
     *
     * @param vr       the validation request containing details like image requirements and other necessary parameters
     * @param name     the identifier used for logging purposes
     * @param actual   the computed polychromy value to validate against the expected range
     * @param expected the expected polychromy definition, including the allowable range for validation
     * @return a RangeResult indicating whether the validation passed or failed, and the failure type if applicable
     */
    @SuppressWarnings({"MethodWithMultipleReturnPoints", "DataFlowIssue", "NonBooleanMethodNameMayNotStartWithQuestion"})
    private static RangeResult isOk(final Request vr, final String name, final Fraction actual, final _Polychromy expected) {

        if (isNull(expected.getRange()) || isNull(expected.getRange().min()) || isNull(expected.getRange().max()))
            return logPolychromyUndefinedBoundsFailure(name);

        if (actual.doubleValue() < expected.getRange().min().doubleValue() || actual.doubleValue() > expected.getRange().max().doubleValue()) {

            final _RangedMargin margin = rm(raf(Allowed.defaultPolychromyMargin, ABSOLUTE));
            final Noun noun = new Noun(POLYCHROMY, null);
            final Subject subject = new Subject(noun, Method.COMPULSIVE);
            final ValidationMeta meta = new ValidationMeta(S0, subject, margin, 0, vr);
            return logOutOfBounds(actual, IMAGE, v(expected.getRange()), meta);
        }

        return RangeResult.OK;
    }

    /**
     * Validates if the polychromy value of a grid adheres to the expected range defined in the request.
     *
     * @param vr   the validation request containing the image requirements, ranges, and other necessary details
     * @param grid the pixel grid to be analyzed for polychromy calculations
     * @return a RangeResult indicating whether the polychromy value is within the acceptable range or not
     */
    @SuppressWarnings({"StaticMethodOnlyUsedInOneClass", "NonBooleanMethodNameMayNotStartWithQuestion"})
    public static RangeResult isPolychromyOk(final Request vr, final _Grid grid) {

        final String name = vr.name();
        final Map<Integer, Pair<Integer, String>> pixelsByColour = grid.calculator().countPixelsByColor();
        final _Polychromy expected = vr.irq().polychromy();
        final PolychromyMethod method = n(PolychromyMethod.EUCLIDEAN_DISTANCE, vr.irq().polychromy().method());
        final Fraction actual = calculateActual(name, pixelsByColour, method);
        final RangeResult result = isOk(vr, name, actual, expected);
        if (!result.isPassed()) new Processor(actual, expected).processNegativeResult(vr);
        return result;
    }
}
