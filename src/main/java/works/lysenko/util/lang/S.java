package works.lysenko.util.lang;

import static works.lysenko.util.chrs.__.BY;
import static works.lysenko.util.chrs.__.OF;
import static works.lysenko.util.chrs.___.NOT;
import static works.lysenko.util.chrs.___.WAS;
import static works.lysenko.util.chrs.____.CODE;
import static works.lysenko.util.chrs.____.DOES;
import static works.lysenko.util.chrs.____.PAGE;
import static works.lysenko.util.chrs.____.ROOT;
import static works.lysenko.util.data.strs.Bind.b;
import static works.lysenko.util.data.strs.Case.c;
import static works.lysenko.util.data.strs.Case.u;
import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.lang.D.DUE_TO;
import static works.lysenko.util.lang.word.A.ANDROID;
import static works.lysenko.util.lang.word.C.CHECK;
import static works.lysenko.util.lang.word.C.CONFIGURATION;
import static works.lysenko.util.lang.word.D.DISABLED;
import static works.lysenko.util.lang.word.E.ELEMENT;
import static works.lysenko.util.lang.word.E.EXCEPTION;
import static works.lysenko.util.lang.word.E.EXECUTION;
import static works.lysenko.util.lang.word.E.EXIST;
import static works.lysenko.util.lang.word.F.FAILED;
import static works.lysenko.util.lang.word.F.FOLDER;
import static works.lysenko.util.lang.word.H.HOME;
import static works.lysenko.util.lang.word.R.REFERENCE;
import static works.lysenko.util.lang.word.R.REQUEST;
import static works.lysenko.util.lang.word.S.*;
import static works.lysenko.util.spec.Symbols.UND_SCR;
import static works.lysenko.util.spec.Symbols._COLON_;
import static works.lysenko.util.spec.Symbols._SLASH_;

/**
 * Language utility constants starting with S.
 */
@SuppressWarnings({"ClassWithoutLogger", "StaticMethodOnlyUsedInOneClass", "AutoBoxing", "WeakerAccess", "unused"})
public record S() {

    private static final String FOLDER_DOES_NOT_EXIST_CHECK = b(FOLDER, DOES, NOT, s(EXIST, _COLON_), CHECK,
            u(b(UND_SCR, ANDROID, HOME)), s(_SLASH_), u(b(UND_SCR, ANDROID, SDK, ROOT)));

    /** Constant representing scenarios stack. */
    public static final String SCENARIOS_STACK = b(c(SCENARIOS), s(STACK, _COLON_));
    /** Constant representing scenario execution failed due to message. */
    public static final String SCENARIO_EXECUTION_FAILED_DUE_TO = b(c(SCENARIO), EXECUTION, FAILED, DUE_TO);
    /** Constant representing shrinking was disabled by configuration message. */
    public static final String SHRINKING_WAS_DISABLED_BY_CONFIGURATION = b(c(SHRINKING), WAS, DISABLED, BY, CONFIGURATION);
    /** Constant representing shrinking was disabled by request message. */
    public static final String SHRINKING_WAS_DISABLED_BY_REQUEST = b(c(SHRINKING), WAS, DISABLED, BY, REQUEST);
    /** Constant representing snapshot of page code. */
    public static final String SNAPSHOT_OF_PAGE_CODE = b(SNAPSHOT, OF, PAGE, CODE);
    /** Constant representing StaleElementReferenceException name. */
    public static final String S_E_R_EXCEPTION = s(c(STALE), c(ELEMENT), c(REFERENCE), c(EXCEPTION));
    /** Constant representing SDK root check message. */
    public static final String SDK_ROOT_CHECK = b(c(ANDROID), u(SDK), ROOT, FOLDER_DOES_NOT_EXIST_CHECK);
    /** Constant representing SDK folder check message. */
    public static final String SDK_FOLDER_CHECK = b(c(ANDROID), u(SDK), FOLDER_DOES_NOT_EXIST_CHECK);
    /** Constant representing SDK root check diagnostic message. */
    public static final String MSG_SDK_ROOT_CHECK = SDK_ROOT_CHECK;
    /** Constant representing SDK folder check diagnostic message. */
    public static final String MSG_SDK_FOLDER_CHECK = SDK_FOLDER_CHECK;
}
