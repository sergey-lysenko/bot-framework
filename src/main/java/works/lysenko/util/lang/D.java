package works.lysenko.util.lang;

import static works.lysenko.util.chrs.__.IS;
import static works.lysenko.util.chrs.__.OF;
import static works.lysenko.util.chrs.__.TO;
import static works.lysenko.util.chrs.___.*;
import static works.lysenko.util.chrs.____.DOES;
import static works.lysenko.util.data.strs.Bind.b;
import static works.lysenko.util.data.strs.Case.c;
import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.lang.word.A.AMOUNT;
import static works.lysenko.util.lang.word.C.CLASS;
import static works.lysenko.util.lang.word.D.*;
import static works.lysenko.util.lang.word.G.GROUND;
import static works.lysenko.util.lang.word.L.LINES;
import static works.lysenko.util.spec.Symbols.*;


@SuppressWarnings({"ClassWithoutLogger", "MissingJavadoc", "StaticMethodOnlyUsedInOneClass", "AutoBoxing", "unused"})
public record D() {

    private static final String DIDN_T = s(DID, N, _QUOTE_, T);
    public static final String DIDN_T_GET_OFF_THE_GROUND = b(c(DIDN_T), GET, OFF, THE, GROUND);
    public static final String DIFFERENCE_IS = b(DIFFERENCE, IS);
    public static final String DIFFERENT_AMOUNT_OF = b(c(DIFFERENT), AMOUNT, OF);
    public static final String DOES_NOT_FIT = b(DOES, NOT, FIT);
    public static final String DOT_CLASS = s(_DOT_, CLASS);
    public static final String DUE_TO = b(DUE, TO);
    public static final String DUPLICATED___ = b(c(DUPLICATED), LOG, s(LINES, _COLON_));
}
