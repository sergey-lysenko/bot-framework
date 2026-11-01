package works.lysenko.util.lang;

import org.apache.commons.lang3.StringUtils;

import static works.lysenko.util.chrs.__.*;
import static works.lysenko.util.chrs.___.*;
import static works.lysenko.util.chrs.____.*;
import static works.lysenko.util.chrs._____.VALUE;
import static works.lysenko.util.data.strs.Bind.b;
import static works.lysenko.util.data.strs.Bind.d;
import static works.lysenko.util.data.strs.Case.c;
import static works.lysenko.util.data.strs.Case.u;
import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.lang.C.COLOURS_RESULT;
import static works.lysenko.util.lang.D.DUE_TO;
import static works.lysenko.util.lang.R.RANGER_RESULT;
import static works.lysenko.util.lang.W.WEB_DRIVER;
import static works.lysenko.util.lang.word.A.AMONG;
import static works.lysenko.util.lang.word.A.AUTOMATOR;
import static works.lysenko.util.lang.word.B.BROWSER;
import static works.lysenko.util.lang.word.B.BUNDLE;
import static works.lysenko.util.lang.word.B.BYTES;
import static works.lysenko.util.lang.word.C.CANDIDATES;
import static works.lysenko.util.lang.word.C.CLIPBOARD;
import static works.lysenko.util.lang.word.C.CONFIGURED;
import static works.lysenko.util.lang.word.D.DEFINE;
import static works.lysenko.util.lang.word.D.DRIVER;
import static works.lysenko.util.lang.word.D.DURING;
import static works.lysenko.util.lang.word.E.*;
import static works.lysenko.util.lang.word.F.FAILED;
import static works.lysenko.util.lang.word.H.HEADER;
import static works.lysenko.util.lang.word.H.HTTP;
import static works.lysenko.util.lang.word.I.IMAGE;
import static works.lysenko.util.lang.word.I.INCORRECTLY;
import static works.lysenko.util.lang.word.I.INSTANCE;
import static works.lysenko.util.lang.word.I.INSTANTIATION;
import static works.lysenko.util.lang.word.L.LINES;
import static works.lysenko.util.lang.word.M.MARGIN;
import static works.lysenko.util.lang.word.N.NEITHER;
import static works.lysenko.util.lang.word.P.PARSE;
import static works.lysenko.util.lang.word.P.PARSER;
import static works.lysenko.util.lang.word.P.PROCEED;
import static works.lysenko.util.lang.word.P.PROCESS;
import static works.lysenko.util.lang.word.R.RANDOM;
import static works.lysenko.util.lang.word.R.RANGED;
import static works.lysenko.util.lang.word.R.RECEIVED;
import static works.lysenko.util.lang.word.R.RESULT;
import static works.lysenko.util.lang.word.S.*;
import static works.lysenko.util.lang.word.T.TESTS;
import static works.lysenko.util.lang.word.T.TRANSFORM;
import static works.lysenko.util.lang.word.T.TYPES;
import static works.lysenko.util.lang.word.U.*;
import static works.lysenko.util.lang.word.V.VALID;
import static works.lysenko.util.lang.word.V.VALIDATE;
import static works.lysenko.util.lang.word.V.VERIFICATION;
import static works.lysenko.util.lang.word.W.WITHOUT;
import static works.lysenko.util.lang.word.W.WRITE;
import static works.lysenko.util.spec.Symbols.*;

/**
 * Language utility constants starting with U.
 */
@SuppressWarnings({"ClassWithoutLogger", "StaticMethodOnlyUsedInOneClass", "AutoBoxing", "WeakerAccess"})
public record U() {

    private static final String UI_VERIFICATION_FAILED = b(u(UI), VERIFICATION, FAILED);
    /** Constant representing UiAutomator2 driver name. */
    public static final String UI_AUTOMATOR_2 = s(c(UI), AUTOMATOR, _2_);
    /** Constant representing UI verification failed in message. */
    public static final String UI_VERIFICATION_FAILED_IN = b(UI_VERIFICATION_FAILED, IN);
    /** Constant representing the phrase 'unable to'. */
    public static final String UNABLE_TO = b(c(UNABLE), TO);
    /** Constant representing unable to add ok to non empty result message. */
    public static final String UNABLE_TO_ADD_OK___ = b(UNABLE_TO, ADD, u(OK), TO, d(NON, EMPTY), RESULT);
    /** Constant representing unable to define scale due to null image message. */
    public static final String UNABLE_TO_DEFINE___ = b(UNABLE_TO, DEFINE, SCALE, DUE_TO, NULL, IMAGE);
    /** Constant representing unable to get external event types message. */
    public static final String UNABLE_TO_GET_EXTERNAL___ = b(c(UNABLE), TO, GET, EXTERNAL, EVENT, TYPES);
    /** Constant representing the phrase 'unable to load'. */
    public static final String UNABLE_TO_LOAD = b(UNABLE_TO, LOAD);
    /** Constant representing the phrase 'unable to open'. */
    public static final String UNABLE_TO_OPEN = b(UNABLE_TO, OPEN);
    /** Constant representing unable to parse ranged margin from message. */
    public static final String UNABLE_TO_PARCE_RANGEDMARGIN___ = b(UNABLE_TO, PARSE, s(c(RANGED), c(MARGIN)), FROM);
    /** Constant representing unable to proceed due to unknown message. */
    public static final String UNABLE_TO_PROCEED = b(UNABLE_TO, PROCEED, DUE_TO, UNKNOWN);
    /** Constant representing unable to process empty result message. */
    public static final String UNABLE_TO_PROCESS_EMPTY___ = b(UNABLE_TO, PROCESS, EMPTY, RESULT);
    /** Constant representing unable to process null result message. */
    public static final String UNABLE_TO_PROCESS_NULL___ = b(UNABLE_TO, PROCESS, NULL, RESULT);
    /** Constant representing unable to read clipboard message. */
    public static final String UNABLE_TO_READ_CLIPBOARD = b(UNABLE_TO, READ, CLIPBOARD);
    /** Constant representing unable to read lines from message. */
    public static final String UNABLE_TO_READ_LINES_FROM = b(UNABLE_TO, READ, LINES, FROM);
    /** Constant representing unable to read screenshot due to message. */
    public static final String UNABLE_TO_READ_SCREENSHOT___ = b(UNABLE_TO, READ, SCREENSHOT, DUE_TO);
    /** Constant representing unable to read screenshot without valid web driver instance message. */
    public static final String UNABLE_TO_READ___ = b(UNABLE_TO, READ, SCREENSHOT, WITHOUT, VALID, WEB_DRIVER, INSTANCE);
    /** Constant representing unable to select a random element of an empty set message. */
    public static final String UNABLE_TO_SELECT_A_RANDOM_ELEMENT_OF_AN_EMPTY_SET = b(UNABLE_TO, SELECT, s(A), RANDOM, ELEMENT, OF, AN, EMPTY, SET);
    /** Constant representing unable to select a scenario among candidates message. */
    public static final String UNABLE_TO_SELECT_A_SCENARIO___ = b(UNABLE_TO, SELECT, s(A), SCENARIO, AMONG, CANDIDATES);
    /** Constant representing unable to start test without neither app file nor bundle id message. */
    public static final String UNABLE_TO_START_TEST___ = b(UNABLE_TO, START, TEST, WITHOUT, NEITHER, APP, FILE, NOR, BUNDLE, ID);
    /** Constant representing unable to transform ranger result into colours result message. */
    public static final String UNABLE_TO_TRANSFORM___ = b(UNABLE_TO, TRANSFORM, RANGER_RESULT, INTO, COLOURS_RESULT);
    /** Constant representing unable to validate incorrectly configured message. */
    public static final String UNABLE_TO_VALIDATE_INCORRECTLY_CONFIGURED = b(UNABLE_TO, VALIDATE, INCORRECTLY, CONFIGURED);
    /** Constant representing the phrase 'unable to write'. */
    public static final String UNABLE_TO_WRITE = b(UNABLE_TO, WRITE);
    /** Constant representing unable to write code snapshot to message. */
    public static final String UNABLE_TO_WRITE_CODE___ = b(UNABLE_TO, WRITE, CODE, SNAPSHOT, TO);
    /** Constant representing unable to write data to message. */
    public static final String UNABLE_TO_WRITE_DATA_TO = b(UNABLE_TO, WRITE, DATA, TO);
    /** Constant representing unable to write into file message. */
    public static final String UNABLE_TO_WRITE_INTO_FILE = b(UNABLE_TO, WRITE, INTO, FILE);
    /** Constant representing unable to write screenshot message. */
    public static final String UNABLE_TO_WRITE_SCREENSHOT = b(UNABLE_TO, WRITE, SCREENSHOT);
    /** Constant representing uncaught exception during tests execution message. */
    public static final String UNCAUGHT_EXCEPTION = b(c(UNCAUGHT), EXCEPTION, DURING, TESTS, s(EXECUTION, _COLON_));
    /** Constant representing unexpected null value among message. */
    public static final String UNEXPECTED_NULL_VALUE_AMONG_ = b(c(UNEXPECTED), NULL, VALUE, AMONG, StringUtils.EMPTY);
    /** Constant representing unresolved exceptional state message. */
    public static final String UNRESOLVED___ = b(c(UNRESOLVED), EXCEPTIONAL, STATE);
    /** Constant representing user.dir system property key. */
    public static final String USER_DIR = s(USER, _DOT_, s(DI, R));
    /** Constant representing UnreachableBrowserException name. */
    public static final String U_B_EXCEPTION = s(c(UNREACHABLE), c(BROWSER), c(EXCEPTION));
    /** Constant representing unknown error message. */
    public static final String UNKNOWN_DRIVER_ERROR = b(c(UNKNOWN), DRIVER, INSTANTIATION, ERROR);
    /** Constant representing unknown driver error diagnostic message. */
    public static final String MSG_UNKNOWN_ERROR = UNKNOWN_DRIVER_ERROR;
    /** Constant representing HTTP/1.1 header parser received no bytes diagnostic message. */
    public static final String MSG_PARSER_NO_BYTES = b(s(u(HTTP), _SLASH_, _1_, _DOT_, _1_), HEADER, PARSER, RECEIVED, NO, BYTES);
}
