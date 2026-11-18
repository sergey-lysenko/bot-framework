package works.lysenko.util.spec;


import org.apache.commons.math3.fraction.Fraction;

import static works.lysenko.util.data.strs.Swap.inn;
import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.func.type.fractions.Factory.fr;

/**
 * Utility record defining named integer and fraction mathematical constants.
 */
@SuppressWarnings({"unused", "WeakerAccess", "StaticMethodOnlyUsedInOneClass"})
public record Numbers() {

    /** Integer zero. */
    public static final int ZERO = inn(s(Fraction.ZERO));
    /** Integer one. */
    public static final int ONE = inn(s(Fraction.ONE));
    /** Integer two. */
    public static final int TWO = ONE << ONE;
    /** Integer three. */
    public static final int THREE = TWO + ONE;
    /** Integer five. */
    public static final int FIVE = THREE + TWO;
    /** Integer seven. */
    public static final int SEVEN = FIVE + TWO;
    /** Integer nine. */
    public static final int NINE = SEVEN + TWO;
    /** Integer ten. */
    public static final int TEN = FIVE << ONE;
    /** Integer hundred. */
    public static final int HUNDRED = TEN * TEN;
    /** Integer six. */
    public static final int SIX = THREE << ONE;
    /** Integer four. */
    public static final int FOUR = TWO << ONE;
    /** Integer eight. */
    public static final int EIGHT = FOUR << ONE;
    /** Fraction one eighth (1/8). */
    public static final Fraction ONE_EIGHTS = fr(ONE, EIGHT);
    /** Fraction seven eighths (7/8). */
    public static final Fraction SEVEN_EIGHTS = fr(SEVEN, EIGHT);

    /** Frequency indicator once (1). */
    public static final int ONCE = ONE;
    /** Frequency indicator twice (2). */
    public static final int TWICE = TWO;
    /** Frequency indicator thrice (3). */
    public static final int THRICE = THREE;

    /** Absolute value indicator. */
    public static final int ABSOLUTE = ONE;
}
