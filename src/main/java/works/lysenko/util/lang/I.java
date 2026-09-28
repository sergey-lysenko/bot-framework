package works.lysenko.util.lang;

import static works.lysenko.util.chrs.__.*;
import static works.lysenko.util.chrs.___.FOR;
import static works.lysenko.util.chrs.___.NOT;
import static works.lysenko.util.chrs.___.TOO;
import static works.lysenko.util.chrs.____.*;
import static works.lysenko.util.data.strs.Bind.b;
import static works.lysenko.util.data.strs.Case.c;
import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.lang.T.TO_BE;
import static works.lysenko.util.lang.word.A.ALLOWED;
import static works.lysenko.util.lang.word.A.ALREADY;
import static works.lysenko.util.lang.word.B.BIGGER;
import static works.lysenko.util.lang.word.B.BOUNDARIES;
import static works.lysenko.util.lang.word.C.CLOSE;
import static works.lysenko.util.lang.word.C.COMPRESSED;
import static works.lysenko.util.lang.word.D.DEFINED;
import static works.lysenko.util.lang.word.E.EDGE;
import static works.lysenko.util.lang.word.E.EXECUTED;
import static works.lysenko.util.lang.word.I.INTEGER;
import static works.lysenko.util.lang.word.I.INVALID;
import static works.lysenko.util.lang.word.M.MUCH;
import static works.lysenko.util.lang.word.O.OPERATION;
import static works.lysenko.util.lang.word.S.SINGLE;
import static works.lysenko.util.lang.word.W.WIDER;
import static works.lysenko.util.spec.Symbols.A;

@SuppressWarnings({"ClassWithoutLogger", "MissingJavadoc", "StaticMethodOnlyUsedInOneClass", "AutoBoxing"})
public record I() {

    public static final String INITIAL_SETBACK_MESSAGE = "Невдачка спіткала українських спортсменів на самому старті"; //NON-NLS
    public static final String INVALID_COMPRESSED_DATA = b(c(INVALID), COMPRESSED, DATA);
    public static final String IS_ALREADY_ON_EDGE_OF = b(IS, ALREADY, ON, EDGE, OF);
    public static final String IS_BIGGER_THAN_ALLOWED = b(IS, BIGGER, THAN, ALLOWED);
    public static final String IS_DEFINED_MORE_THAN___ = b(IS, DEFINED, MORE, THAN, ONCE, IN);
    public static final String IS_NOT_AN_INTEGER_IN___ = b(IS, NOT, AN, INTEGER, IN);
    public static final String IS_TOO_CLOSE_TO___ = b(IS, TOO, CLOSE, TO, BOUNDARIES, OF);
    public static final String IS_TOO_MUCH_TIME___ = b(IS, TOO, MUCH, TIME, FOR, s(A), SINGLE, OPERATION);
    public static final String IS_TO_BE_EXECUTED = b(IS, TO_BE, EXECUTED);
    public static final String IS_WIDER_THAN = b(IS, WIDER, THAN);
}
