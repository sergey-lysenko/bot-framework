package works.lysenko.util.lang;

import static works.lysenko.util.chrs.__.OF;
import static works.lysenko.util.chrs.__.ON;
import static works.lysenko.util.chrs.__.OR;
import static works.lysenko.util.chrs.___.ANY;
import static works.lysenko.util.chrs.___.DOTS;
import static works.lysenko.util.chrs.___.THE;
import static works.lysenko.util.chrs.____.NODE;
import static works.lysenko.util.chrs.____.ROOT;
import static works.lysenko.util.chrs.____.TEST;
import static works.lysenko.util.spec.Synth.*;
import static works.lysenko.util.lang.word.C.*;
import static works.lysenko.util.lang.word.D.DURING;
import static works.lysenko.util.lang.word.E.EXECUTION;
import static works.lysenko.util.lang.word.N.NAVIGATOR;
import static works.lysenko.util.lang.word.R.RESULT;
import static works.lysenko.util.lang.word.S.SERVICE;
import static works.lysenko.util.lang.word.T.TABLE;
import static works.lysenko.util.lang.word.T.TREE;
import static works.lysenko.util.lang.word.V.VISIBILITY;
import static works.lysenko.util.spec.Symbols._COMMA_;
import static works.lysenko.util.spec.Symbols._SPACE_;


/**
 * Language utility constants starting with C.
 */
@SuppressWarnings({"ClassWithoutLogger", "StaticMethodOnlyUsedInOneClass"})
public record C() {

    /** Constant representing card layout key 'TABLE'. */
    public static final String CARD_TABLE = u(TABLE);
    /** Constant representing card layout key 'TREE'. */
    public static final String CARD_TREE = u(TREE);
    /** Constant representing the phrase 'checking visibility of'. */
    public static final String CHECKING_VISIBILITY_OF = b(c(CHECKING), VISIBILITY, OF);
    /** Constant representing the hint 'Click any node on the canvas or navigator'. */
    public static final String CLICK_ANY_NODE___ = b(c(CLICK), ANY, NODE, ON, THE, CANVAS, OR, NAVIGATOR);
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
