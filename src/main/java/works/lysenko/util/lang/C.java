package works.lysenko.util.lang;

import static works.lysenko.util.chrs.__.OF;
import static works.lysenko.util.chrs.__.ON;
import static works.lysenko.util.chrs.___.DOTS;
import static works.lysenko.util.chrs.____.ROOT;
import static works.lysenko.util.chrs.____.TEST;
import static works.lysenko.util.data.strs.Bind.b;
import static works.lysenko.util.data.strs.Case.c;
import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.lang.word.C.*;
import static works.lysenko.util.lang.word.D.DURING;
import static works.lysenko.util.lang.word.E.EXECUTION;
import static works.lysenko.util.lang.word.R.RESULT;
import static works.lysenko.util.lang.word.S.SERVICE;
import static works.lysenko.util.lang.word.V.VISIBILITY;
import static works.lysenko.util.spec.Symbols._COMMA_;
import static works.lysenko.util.spec.Symbols._SPACE_;


/**
 * Language utility constants starting with C.
 */
@SuppressWarnings({"ClassWithoutLogger", "StaticMethodOnlyUsedInOneClass"})
public record C() {

    /** Constant representing the phrase 'checking visibility of'. */
    public static final String CHECKING_VISIBILITY_OF = b(c(CHECKING), VISIBILITY, OF);
    /** Constant representing the phrase 'clicking on'. */
    public static final String CLICKING_ON = b(c(CLICKING), ON);
    /** Constant representing comma followed by a space. */
    public static final String COMMA_SPACE = s(_COMMA_, _SPACE_);
    /** Constant representing colours result. */
    public static final String COLOURS_RESULT = s(c(COLOURS), c(RESULT));
    /** Constant representing the phrase 'caught during execution of'. */
    public static final String CAUGHT_DURING_EXECUTION_OF = b(CAUGHT, DURING, EXECUTION, OF);
    /** Constant representing the message for closing test service. */
    public static final String CLOSING_TEST_SERVICE = b(c(CLOSING), TEST, SERVICE, DOTS);
}
