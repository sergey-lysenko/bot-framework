package works.lysenko.util.grid.validation;

import org.apache.commons.math3.fraction.Fraction;
import org.apache.commons.math3.util.Precision;
import works.lysenko.util.apis.grid.d._Resolution;
import works.lysenko.util.apis.grid.g._GridProperties;
import works.lysenko.util.apis.grid.t._RelativePosition;
import works.lysenko.util.apis.grid.v._ValidationResult;
import works.lysenko.util.data.records.KeyValue;
import works.lysenko.util.data.type.sets.GridValidationResults;
import works.lysenko.util.grid.record.Request;
import works.lysenko.util.prop.grid.Defaults;
import works.lysenko.util.prop.grid.Validation;

import java.util.ArrayList;
import java.util.List;

import static java.util.Objects.isNull;
import static works.lysenko.Base.log;
import static works.lysenko.Base.logDebug;
import static works.lysenko.Base.logEvent;
import static works.lysenko.util.apis.grid.d._Resolution.readResolution;
import static works.lysenko.util.chrs.__.IS;
import static works.lysenko.util.chrs.__.OF;
import static works.lysenko.util.chrs.__.TO;
import static works.lysenko.util.chrs.___.FOR;
import static works.lysenko.util.chrs.___.THE;
import static works.lysenko.util.chrs.___.WAS;
import static works.lysenko.util.chrs.____.GRID;
import static works.lysenko.util.chrs.____.NULL;
import static works.lysenko.util.data.enums.Ansi.gb;
import static works.lysenko.util.data.enums.Ansi.rb;
import static works.lysenko.util.data.enums.Ansi.yb;
import static works.lysenko.util.data.enums.GridValidationResult.INVALID_GRID;
import static works.lysenko.util.data.enums.GridValidationResult.INVALID_REQUEST;
import static works.lysenko.util.data.enums.Severity.S0;
import static works.lysenko.util.data.enums.Severity.S3;
import static works.lysenko.util.data.records.KeyValue.kv;
import static works.lysenko.util.data.strs.Bind.b;
import static works.lysenko.util.data.strs.Case.c;
import static works.lysenko.util.data.strs.Null.n;
import static works.lysenko.util.data.strs.Swap.d;
import static works.lysenko.util.data.strs.Swap.qinn;
import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.data.strs.Vars.a;
import static works.lysenko.util.data.strs.Wrap.q;
import static works.lysenko.util.data.type.RelativePosition.rp;
import static works.lysenko.util.func.type.Objects.isNotNull;
import static works.lysenko.util.func.type.fractions.Factory.fr;
import static works.lysenko.util.func.type.fractions.Render.ts;
import static works.lysenko.util.lang.C.COMMA_SPACE;
import static works.lysenko.util.lang.word.C.CONFIGURED;
import static works.lysenko.util.lang.word.E.EFFECTIVE;
import static works.lysenko.util.lang.word.F.FAILED;
import static works.lysenko.util.lang.word.H.HORIZONTAL;
import static works.lysenko.util.lang.word.I.INCORRECTLY;
import static works.lysenko.util.lang.word.M.MARGIN;
import static works.lysenko.util.lang.word.P.PASSED;
import static works.lysenko.util.lang.word.R.*;
import static works.lysenko.util.lang.word.S.SCALE;
import static works.lysenko.util.lang.word.U.UNABLE;
import static works.lysenko.util.lang.word.V.VALIDATE;
import static works.lysenko.util.lang.word.V.VALIDATION;
import static works.lysenko.util.lang.word.V.VERTICAL;
import static works.lysenko.util.lang.word.W.WHILE;
import static works.lysenko.util.prop.grid.Defaults.horizontal;
import static works.lysenko.util.prop.grid.Defaults.vertical;
import static works.lysenko.util.spec.Numbers.TWO;
import static works.lysenko.util.spec.Symbols._COMMA_;
import static works.lysenko.util.spec.Symbols._PRCNT_;

/**
 *
 */
@SuppressWarnings("StaticMethodOnlyUsedInOneClass")
public record Routines() {

    private static final double DEFAULT_SCALE_EPSILON = 0.0001;
    private static final String B1 = b(c(REQUESTED), SCALE, FOR, THE);
    private static final String B2 = b(VALIDATION, WAS);
    private static final String B3 = b(WHILE, EFFECTIVE, SCALE, IS);

    /**
     * Retrieves the string for log based on the provided Request object.
     */
    private static String getB(final String name) {

        return b(c(VALIDATION), OF, qinn(name, GRID));
    }

    /**
     * Retrieves the RelativePosition object from the provided Properties file.
     *
     * @param gridProperties the Properties file containing the horizontal and vertical properties
     * @return a RelativePosition object representing the position retrieved from the Properties file
     */
    static _RelativePosition getRelativePosition(final _GridProperties gridProperties) {

        return rp(gridProperties.getKV(HORIZONTAL, horizontal).v(), gridProperties.getKV(VERTICAL, vertical).v());
    }

    /**
     * Retrieves the Resolution object from the provided Properties file.
     *
     * @param gridProperties the Properties file containing the resolution property
     * @return a Resolution object representing the resolution retrieved from the Properties file
     */
    static _Resolution getResolution(final _GridProperties gridProperties) {

        return readResolution(gridProperties.getKV(RESOLUTION, Defaults.resolution).v());
    }

    /**
     * Retrieves the scale as a Fraction object from the provided Properties file.
     *
     * @param gridProperties the Properties file containing the scale property
     * @return a Fraction object representing the scale retrieved from the Properties file
     */
    static Fraction getScale(final _GridProperties gridProperties) {

        return fr(gridProperties.getKV(SCALE, Defaults.scale).v());
    }

    /**
     * Logs the result of a validation process along with the name of the validation and its outcome.
     *
     * @param name   the name of the validation process
     * @param result the validation result object representing the outcome of the process
     */
    public static void logResult(final String name, final _ValidationResult result) {

        if (isNotNull(result)) {
            log(b(c(RESULT), IS, result.render()));
            if (result.isPassed()) log(gb(b(getB(name), PASSED)));
            else log(rb(b(getB(name), FAILED)));
        }
    }

    /**
     * Checks if the provided scale value matches the scale value in the Request object.
     * If the scale values do not match within the defined epsilon threshold, a log event is generated.
     *
     * @param request the Request object containing scale information
     * @param scale   the scale value to compare against the scale value in the request
     */
    public static void logScaleCheck(final Request request, final Fraction scale) {

        if (!Precision.equals(request.scale().doubleValue(), scale.doubleValue(), n(DEFAULT_SCALE_EPSILON,
                Validation.scaleEpsilon)))
            logEvent(S3, b(B1, q(request.name()), B2, s(ts(request.scale()), _COMMA_), B3, s(ts(scale))));
    }

    /**
     * Logs the debug information about the scaling process using the provided Request and scale values.
     *
     * @param request the validation request containing the pixel grid and scale information
     * @param scale   the effective scale value used in the scaling process
     */
    public static void logScaleDebug(final Request request, final Fraction scale) {

        final List<KeyValue<?, ?>> debug = new ArrayList<>(2);
        debug.add(kv(REQUESTED, yb(ts(request.scale()))));
        debug.add(kv(EFFECTIVE, yb(ts(scale))));
        if (0 != request.scale().doubleValue()) {
            final double margin = 100 - scale.divide(request.scale()).percentageValue();
            if (0 < margin) debug.add(kv(MARGIN, yb(s(d(margin, TWO), _PRCNT_))));
        }
        logDebug(a(debug, COMMA_SPACE));
    }

    /**
     * Validates the GridResult based on the given Request.
     *
     * @param request the validation request containing the pixel grid information
     * @return the validated GridResult, may be INVALID_REQUEST, INVALID_GRID, or null
     */
    public static GridValidationResults validateRequest(final Request request) {

        final GridValidationResults results = new GridValidationResults();
        if (isNull(request)) {
            logEvent(S0, b(c(UNABLE), TO, VALIDATE, NULL, c(VALIDATION), c(REQUEST)));
            results.add(INVALID_REQUEST);
        }

        if (isNull(request.irq())) {
            logEvent(S0, b(c(UNABLE), TO, VALIDATE, INCORRECTLY, CONFIGURED, q(request.name()), GRID));
            results.add(INVALID_GRID);
        }
        return results;
    }
}
