package works.lysenko.util.lang.word;

import static works.lysenko.util.chrs.__.*;
import static works.lysenko.util.chrs.___.*;
import static works.lysenko.util.chrs.____.NEGA;
import static works.lysenko.util.chrs.____.NOTI;
import static works.lysenko.util.chrs.____.TIVE;
import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.spec.Symbols.H;
import static works.lysenko.util.spec.Symbols.M;
import static works.lysenko.util.spec.Symbols.R;
import static works.lysenko.util.spec.Symbols.S;

/**
 * Language utility words starting with N.
 */
@SuppressWarnings({"StaticMethodOnlyUsedInOneClass"})
public record N() {

    /** Constant representing the word NAMED. */
    public static final String NAMED = s(NA, M, ED);
    /** Constant representing the word NATIVE. */
    public static final String NATIVE = s(NA, TIVE);
    /** Constant representing the word NEGATIVE. */
    public static final String NEGATIVE = s(NEGA, TIVE);
    /** Constant representing the word NEITHER. */
    public static final String NEITHER = s(NE, IT, HER);
    /** Constant representing the word NESTED. */
    public static final String NESTED = s(NE, ST, ED);
    /** Constant representing the word NEUTRAL. */
    public static final String NEUTRAL = s(NE, UT, R, AL);
    /** Constant representing the word NONE. */
    public static final String NONE = s(NO, NE);
    /** Constant representing the word NOTHING. */
    public static final String NOTHING = s(NOT, H, ING);
    /** Constant representing the word NOTICE. */
    public static final String NOTICE = s(NOTI, CE);
    /** Constant representing the word NOTICES. */
    public static final String NOTICES = s(NOTICE, S);
}
