package works.lysenko.util.func.core;

import org.apache.commons.math3.fraction.Fraction;
import works.lysenko.util.apis.grid.q._Quota;
import works.lysenko.util.data.enums.RangeResult;
import works.lysenko.util.data.enums.Severity;

import static works.lysenko.Base.logEvent;
import static works.lysenko.util.chrs.__.IN;
import static works.lysenko.util.chrs.__.IS;
import static works.lysenko.util.data.enums.Severity.S0;
import static works.lysenko.util.data.enums.Severity.S2;
import static works.lysenko.util.data.strs.Bind.b;
import static works.lysenko.util.data.strs.Case.c;
import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.data.strs.Wrap.q;
import static works.lysenko.util.lang.word.B.BOUNDS;
import static works.lysenko.util.lang.word.I.IGNORED;
import static works.lysenko.util.lang.word.M.METHOD;
import static works.lysenko.util.lang.word.P.POLYCHROMY;
import static works.lysenko.util.lang.word.U.UNDEFINED;

/**
 * Provides event logging mechanisms for different scenarios in a system.
 * Contains methods to log specific types of events with relevant information.
 */
@SuppressWarnings({"StaticMethodOnlyUsedInOneClass", "BooleanMethodNameMustStartWithQuestion"})
public record Events() {

    // TODO: rework and unify taxonomy and behaviour

    /**
     * Logs a failure event regarding the distribution rate.
     *
     * @param severity the severity of the event
     * @param message  the message of the event
     * @return whether the failure event was logged
     */
    public static boolean logGenericFailure(final Severity severity, final String message) {

        logEvent(severity, message);
        return false;
    }

    @SuppressWarnings("MissingJavadoc")
    public static void logIgnoring(final String s, final _Quota<?> sh) {

        logEvent(S2, b(s, q(s(sh)), IS, IGNORED));
    }

    /**
     * Logs a failure event when the polychromy value has undefined bounds.
     *
     * @param name the name of the polychromy being checked
     * @return false indicating that the failure event was logged
     */
    public static RangeResult logPolychromyUndefinedBoundsFailure(final String name) {

        logEvent(S0, b(c(UNDEFINED), POLYCHROMY, BOUNDS, IN, q(name)));
        return RangeResult.NOT_OK;
    }

    /**
     * Logs a failure event when a polychromy method is undefined.
     *
     * @param name the name of the polychromy being checked
     * @return false indicating that the failure event was logged
     */
    public static Fraction logPolychromyUndefinedMethodFailure(final String name) {

        logEvent(S0, b(c(UNDEFINED), POLYCHROMY, METHOD, IN, q(name)));
        return null;
    }
}
