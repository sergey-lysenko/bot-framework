package works.lysenko.util.lang;

import static works.lysenko.util.chrs.__.AN;
import static works.lysenko.util.chrs.__.IN;
import static works.lysenko.util.chrs.__.IS;
import static works.lysenko.util.chrs.__.OF;
import static works.lysenko.util.chrs.__.TO;
import static works.lysenko.util.chrs.___.*;
import static works.lysenko.util.chrs.____.DOES;
import static works.lysenko.util.chrs.____.FREE;
import static works.lysenko.util.data.strs.Bind.b;
import static works.lysenko.util.data.strs.Case.c;
import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.lang.word.A.ALREADY;
import static works.lysenko.util.lang.word.A.AMOUNT;
import static works.lysenko.util.lang.word.A.APPIUM;
import static works.lysenko.util.lang.word.A.AVAILABLE;
import static works.lysenko.util.lang.word.C.CLASS;
import static works.lysenko.util.lang.word.D.*;
import static works.lysenko.util.lang.word.G.GROUND;
import static works.lysenko.util.lang.word.L.LINES;
import static works.lysenko.util.lang.word.P.PORT;
import static works.lysenko.util.lang.word.S.SELECTING;
import static works.lysenko.util.lang.word.U.USE;
import static works.lysenko.util.spec.Symbols.*;


/**
 * Language utility constants starting with D.
 */
@SuppressWarnings({"ClassWithoutLogger", "StaticMethodOnlyUsedInOneClass", "AutoBoxing", "unused"})
public record D() {

    private static final String DIDN_T = s(DID, N, _QUOTE_, T);
    /** Constant representing the phrase 'didn't get off the ground'. */
    public static final String DIDN_T_GET_OFF_THE_GROUND = b(c(DIDN_T), GET, OFF, THE, GROUND);
    /** Constant representing the phrase 'difference is'. */
    public static final String DIFFERENCE_IS = b(DIFFERENCE, IS);
    /** Constant representing the phrase 'different amount of'. */
    public static final String DIFFERENT_AMOUNT_OF = b(c(DIFFERENT), AMOUNT, OF);
    /** Constant representing the phrase 'does not fit'. */
    public static final String DOES_NOT_FIT = b(DOES, NOT, FIT);
    /** Constant representing '.class'. */
    public static final String DOT_CLASS = s(_DOT_, CLASS);
    /** Constant representing the phrase 'due to'. */
    public static final String DUE_TO = b(DUE, TO);
    /** Constant representing duplicated log lines prefix. */
    public static final String DUPLICATED___ = b(c(DUPLICATED), LOG, s(LINES, _COLON_));
    /** Constant representing default port busy message. */
    public static final String DEFAULT_PORT_BUSY = b(c(DEFAULT), c(APPIUM), PORT, s(_4_, _7_, _2_, _3_), IS, ALREADY,
            IN, s(USE, SEM_CLN), SELECTING, AN, AVAILABLE, FREE, PORT, DOTS);
}
