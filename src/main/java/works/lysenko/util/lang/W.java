package works.lysenko.util.lang;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static works.lysenko.util.chrs.__.*;
import static works.lysenko.util.chrs.___.*;
import static works.lysenko.util.chrs.____.FULL;
import static works.lysenko.util.chrs.____.WERE;
import static works.lysenko.util.chrs.____.WITH;
import static works.lysenko.util.chrs._____.VALUE;
import static works.lysenko.util.data.enums.Brackets.ROUND;
import static works.lysenko.util.data.enums.Brackets.SQUARE;
import static works.lysenko.util.data.strs.Bind.b;
import static works.lysenko.util.data.strs.Case.c;
import static works.lysenko.util.data.strs.Case.u;
import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.data.strs.Wrap.e;
import static works.lysenko.util.lang.F.FULLSCREEN_IMAGE;
import static works.lysenko.util.lang.word.A.ACTUAL;
import static works.lysenko.util.lang.word.A.ATTEMPT;
import static works.lysenko.util.lang.word.C.CLICK;
import static works.lysenko.util.lang.word.C.CROPPED;
import static works.lysenko.util.lang.word.D.DEFINED;
import static works.lysenko.util.lang.word.D.DRIVER;
import static works.lysenko.util.lang.word.D.DURING;
import static works.lysenko.util.lang.word.I.IMAGE;
import static works.lysenko.util.lang.word.I.INVISIBILITY;
import static works.lysenko.util.lang.word.R.*;
import static works.lysenko.util.lang.word.S.SCREENSHOT;
import static works.lysenko.util.lang.word.S.STILL;
import static works.lysenko.util.lang.word.T.TRYING;
import static works.lysenko.util.lang.word.V.VISIBILITY;
import static works.lysenko.util.lang.word.W.*;
import static works.lysenko.util.spec.Symbols._ASTRS_;
import static works.lysenko.util.spec.Symbols._COMMA_;
import static works.lysenko.util.spec.Symbols._SLASH_;


/**
 * Language utility constants starting with W.
 */
@SuppressWarnings({"ClassWithoutLogger", "StaticMethodOnlyUsedInOneClass", "AutoBoxing"})
public record W() {

    /** Constant representing the phrase 'waiting for'. */
    public static final String WAITING_FOR = b(c(WAITING), FOR);
    /** Constant representing the phrase 'waiting for invisibility of'. */
    public static final String WAITING_FOR_INVISIBILITY_OF = b(WAITING_FOR, INVISIBILITY, OF);
    /** Constant representing the phrase 'waiting for visibility of'. */
    public static final String WAITING_FOR_VISIBILITY_OF = b(c(WAITING), FOR, VISIBILITY, OF);
    /** Constant representing the phrase 'waiting while'. */
    public static final String WAITING_WHILE = b(c(WAITING), WHILE);
    /** Constant representing the phrase 'was not reachable'. */
    public static final String WAS_NOT_REACHABLE = b(WAS, NOT, REACHABLE);
    /** Constant representing WebDriver hub path suffix. */
    public static final String WD_HUB = s(_SLASH_, WD, _SLASH_, HUB, _SLASH_);
    /** Constant representing the phrase 'were reproduced'. */
    public static final String WERE_REPRODUCED = b(WERE, REPRODUCED);
    /** Constant representing the phrase 'while trying to'. */
    public static final String WHILE_TRYING_TO = b(WHILE, TRYING, TO);
    /** Constant representing while trying to click during attempt message. */
    public static final String WHILE_TRYING_TO_CLICK_ = b(WHILE_TRYING_TO, s(CLICK, e(ROUND, EMPTY), _COMMA_), DURING,
            ATTEMPT);
    /** Constant representing the phrase 'width is still'. */
    public static final String WIDTH_IS_STILL = b(WIDTH, IS, STILL);
    /** Constant representing width was reduced by actual value message. */
    public static final String WIDTH_WAS_REDUCED_BY___ = b(WIDTH, WAS, REDUCED, BY, ACTUAL, VALUE);
    /** Constant representing the phrase 'with no defined ranges'. */
    public static final String WITH_NO_DEFINED_RANGES = b(WITH, NO, DEFINED, RANGES);
    /** Constant representing the phrase 'writing full screenshot'. */
    public static final String WRITING_FULL_SCREENSHOT = b(c(WRITING), FULL, SCREENSHOT);
    /** Constant representing the phrase 'writing cropped image'. */
    public static final String WRITING_CROPPED_IMAGE = b(c(WRITING), CROPPED, IMAGE);
    /** Constant representing the phrase 'writing fullscreen image'. */
    public static final String WRITING_FULLSCREEN_IMAGE = b(c(WRITING), FULLSCREEN_IMAGE);
    /** Constant representing 'WebDriver'. */
    public static final String WEB_DRIVER = s(c(WEB), c(DRIVER));
    /** Constant representing Markdown warning marker prefix. */
    public static final String WARNING_MARKER = e(e(_ASTRS_, e(_ASTRS_, e(SQUARE, u(WARNING)))), true);
}
