package works.lysenko.util.lang;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static works.lysenko.util.chrs.__.*;
import static works.lysenko.util.chrs.___.*;
import static works.lysenko.util.chrs.____.FULL;
import static works.lysenko.util.chrs.____.WERE;
import static works.lysenko.util.chrs.____.WITH;
import static works.lysenko.util.chrs._____.VALUE;
import static works.lysenko.util.data.enums.Brackets.ROUND;
import static works.lysenko.util.data.strs.Bind.b;
import static works.lysenko.util.data.strs.Case.c;
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
import static works.lysenko.util.spec.Symbols._COMMA_;
import static works.lysenko.util.spec.Symbols._SLASH_;


@SuppressWarnings({"ClassWithoutLogger", "MissingJavadoc", "StaticMethodOnlyUsedInOneClass", "AutoBoxing"})
public record W() {

    public static final String WAITING_FOR = b(c(WAITING), FOR);
    public static final String WAITING_FOR_INVISIBILITY_OF = b(WAITING_FOR, INVISIBILITY, OF);
    public static final String WAITING_FOR_VISIBILITY_OF = b(c(WAITING), FOR, VISIBILITY, OF);
    public static final String WAITING_WHILE = b(c(WAITING), WHILE);
    public static final String WAS_NOT_REACHABLE = b(WAS, NOT, REACHABLE);
    public static final String WD_HUB = s(_SLASH_, WD, _SLASH_, HUB, _SLASH_);
    public static final String WERE_REPRODUCED = b(WERE, REPRODUCED);
    public static final String WHILE_TRYING_TO = b(WHILE, TRYING, TO);
    public static final String WHILE_TRYING_TO_CLICK_ = b(WHILE_TRYING_TO, s(CLICK, e(ROUND, EMPTY), _COMMA_), DURING,
            ATTEMPT);
    public static final String WIDTH_IS_STILL = b(WIDTH, IS, STILL);
    public static final String WIDTH_WAS_REDUCED_BY___ = b(WIDTH, WAS, REDUCED, BY, ACTUAL, VALUE);
    public static final String WITH_NO_DEFINED_RANGES = b(WITH, NO, DEFINED, RANGES);
    public static final String WRITING_FULL_SCREENSHOT = b(c(WRITING), FULL, SCREENSHOT);
    public static final String WRITING_CROPPED_IMAGE = b(c(WRITING), CROPPED, IMAGE);
    public static final String WRITING_FULLSCREEN_IMAGE = b(c(WRITING), FULLSCREEN_IMAGE);
    public static final String WEB_DRIVER = s(c(WEB), c(DRIVER));
}
