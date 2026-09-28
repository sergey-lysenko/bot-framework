package works.lysenko.util.func.type;

import org.apache.commons.math3.fraction.Fraction;

import java.security.SecureRandom;

import static java.util.Objects.isNull;
import static org.apache.commons.math3.fraction.Fraction.ONE_HALF;
import static works.lysenko.util.chrs.__.IS;
import static works.lysenko.util.chrs.__.OF;
import static works.lysenko.util.chrs.___.THE;
import static works.lysenko.util.chrs.____.TRUE;
import static works.lysenko.util.data.strs.Bind.b;
import static works.lysenko.util.data.strs.Case.c;
import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.lang.word.F.FALSE;
import static works.lysenko.util.lang.word.G.GIVEN;
import static works.lysenko.util.lang.word.O.OUTSIDE;
import static works.lysenko.util.lang.word.P.PROBABILITY;
import static works.lysenko.util.lang.word.R.RANGE;
import static works.lysenko.util.lang.word.V.VALID;
import static works.lysenko.util.spec.Symbols.*;


/**
 * Additional Boolean routines
 */
@SuppressWarnings({"ImplicitNumericConversion", "AutoBoxing", "NestedMethodCall", "ClassIndependentOfModule"})
public record Booleans() {

    /**
     * Parses a String value to a Boolean.
     *
     * @param s the String value to be parsed
     * @return the Boolean value parsed from the String, or null if the value cannot be parsed
     */
    @SuppressWarnings({"BooleanMethodNameMustStartWithQuestion", "StaticMethodOnlyUsedInOneClass", "MethodWithMultipleReturnPoints", "CallToSuspiciousStringMethod", "unused"})
    public static Boolean getBooleanOrNull(final String s) {

        if (isNull(s)) return null;
        if (s.equals(TRUE)) return true;
        if (s.equals(FALSE)) return false;
        return null;
    }

    /**
     * Return 'true' of 'false' with equal probability
     *
     * @return random boolean value
     */
    public static boolean isTrue() {

        return isTrue(ONE_HALF);
    }

    /**
     * Determines whether a given probability value, represented as a {@code Fraction}, is considered "true" or "false"
     * based on a random number generation.
     *
     * @param probability The probability value, represented as a {@code Fraction}, to be evaluated.
     * @return {@code true} if the probability value is considered "true" based on the random number generation,
     * {@code false} otherwise.
     */
    public static boolean isTrue(final Fraction probability) {

        return isTrue(probability.doubleValue());
    }

    /**
     * Determines whether a given probability value is considered "true" or "false" based on a random number generation.
     *
     * @param probability The probability value to be evaluated.
     * @return {@code true} if the probability value is considered "true" based on the random number generation, {@code
     * false} otherwise.
     * @throws IllegalArgumentException if the probability value is outside the valid range of 0.0 to 1.0 (inclusive).
     */
    public static boolean isTrue(final double probability) {

        if (1.0 < probability || 0.0 > probability)
            throw new IllegalArgumentException(b(c(GIVEN), PROBABILITY,
                    s(probability), IS, OUTSIDE, THE, VALID, RANGE, OF,
                    s(OPN_SBR, _0_, _DOT_, _0_), s(_DASH_), s(_1_, _DOT_, _0_, CLS_SBR)));
        return new SecureRandom().nextFloat() < probability;
    }
}
