package works.lysenko.util.func.data;

import static works.lysenko.util.data.enums.Brackets.ROUND;
import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.data.strs.Wrap.e;
import static works.lysenko.util.prop.data.Delimeters.L0;
import static works.lysenko.util.prop.data.Delimeters.L1;
import static works.lysenko.util.prop.data.Delimeters.L2;
import static works.lysenko.util.spec.Symbols.*;

/**
 * The Regexes class provides common regular expressions as static constants,
 * along with utility methods for manipulating regular expressions.
 */
@SuppressWarnings({"unused", "MissingJavadoc", "StaticMethodOnlyUsedInOneClass", "NestedMethodCall",
        "ClassWithTooManyTransitiveDependencies", "ClassWithTooManyTransitiveDependents", "AutoBoxing"})
public record Regexes() {

    public static final String whitespace = s(REW_SOL, S);
    public static final String digit = s(REW_SOL, D);
    public static final String l0 = s(REW_SOL, L0);
    public static final String l1 = s(REW_SOL, L1);
    public static final String l2 = s(REW_SOL, L2);
    public static final String colon = s(REW_SOL, _COLON_);
    public static final String opn_brk = s(REW_SOL, OPN_BRK);
    public static final String cls_brk = s(REW_SOL, CLS_BRK);
    public static final String dot = s(REW_SOL, _DOT_);
    public static final String zeroOrOne = s(QUS_MRK);
    public static final String zeroToUnlimited = s(_ASTRS_);
    public static final String oneToUnlimited = s(_PLUS_);

    public static final String digits = s(digit, oneToUnlimited);

    /**
     * Returns a capturing group with the specified regular expression.
     *
     * @param regex the regular expression to include in the group
     * @return the resulting capturing group as a string
     */
    public static String capturingGroup(final String regex) {

        return e(ROUND, regex);
    }

    /**
     * Wraps string in line markers.
     *
     * @param regex to wrap
     * @return the concatenated string
     */
    public static String line(final String regex) {

        return s(CRCMFLX, regex, _DOLLAR);
    }

    /**
     * Returns a non-capturing group with the specified regular expression.
     *
     * @param regex the regular expression to include in the group
     * @return the resulting non-capturing group as a string
     */
    public static String nonCapturingGroup(final String regex) {

        return e(ROUND, s(QUS_MRK, _COLON_, regex));
    }

}
