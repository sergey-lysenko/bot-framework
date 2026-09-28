package works.lysenko.util.spec;


import org.apache.commons.math3.fraction.Fraction;

import static works.lysenko.util.data.strs.Swap.inn;
import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.func.type.fractions.Factory.fr;

/**
 * Named numbers
 */
@SuppressWarnings({"MissingJavadoc", "unused", "WeakerAccess", "StaticMethodOnlyUsedInOneClass"})
public record Numbers() {

    public static final int ZERO = inn(s(Fraction.ZERO));
    public static final int ONE = inn(s(Fraction.ONE));
    public static final int TWO = ONE << ONE;
    public static final int THREE = TWO + ONE;
    public static final int FIVE = THREE + TWO;
    public static final int SEVEN = FIVE + TWO;
    public static final int NINE = SEVEN + TWO;
    public static final int TEN = FIVE << ONE;
    public static final int HUNDRED = TEN * TEN;
    public static final int SIX = THREE << ONE;
    public static final int FOUR = TWO << ONE;
    public static final int EIGHT = FOUR << ONE;
    public static final Fraction ONE_EIGHTS = fr(ONE, EIGHT);
    public static final Fraction SEVEN_EIGHTS = fr(SEVEN, EIGHT);

    public static final int ONCE = ONE;
    public static final int TWICE = TWO;
    public static final int THRICE = THREE;

    public static final int ABSOLUTE = ONE;
}
