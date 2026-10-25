package works.lysenko.util.lang;

import static works.lysenko.util.chrs.__.*;
import static works.lysenko.util.chrs.___.*;
import static works.lysenko.util.chrs.____.*;
import static works.lysenko.util.chrs._____.VALUE;
import static works.lysenko.util.data.strs.Bind.b;
import static works.lysenko.util.data.strs.Bind.d;
import static works.lysenko.util.data.strs.Case.c;
import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.lang.word.A.*;
import static works.lysenko.util.lang.word.C.COLOURS;
import static works.lysenko.util.lang.word.C.COULD;
import static works.lysenko.util.lang.word.C.COUNTER;
import static works.lysenko.util.lang.word.E.ELEMENT;
import static works.lysenko.util.lang.word.G.GIVEN;
import static works.lysenko.util.lang.word.L.LAUNCHER;
import static works.lysenko.util.lang.word.L.LOCATED;
import static works.lysenko.util.lang.word.P.PACKAGE;
import static works.lysenko.util.lang.word.P.PARAMETERS;
import static works.lysenko.util.lang.word.R.RECORDS;
import static works.lysenko.util.lang.word.R.REDEFINED;
import static works.lysenko.util.lang.word.R.RESET;
import static works.lysenko.util.lang.word.S.SEARCH;
import static works.lysenko.util.lang.word.S.SIMILAR;
import static works.lysenko.util.lang.word.U.USING;
import static works.lysenko.util.lang.word.V.VALID;
import static works.lysenko.util.lang.word.V.VALUES;
import static works.lysenko.util.spec.Symbols._COLON_;
import static works.lysenko.util.spec.Symbols._DASH_;
import static works.lysenko.util.spec.Symbols._DOT_;


/**
 * Language utility constants starting with A.
 */
@SuppressWarnings({"ClassWithoutLogger", "StaticMethodOnlyUsedInOneClass", "AutoBoxing"})
public record A() {

    private static final String APPIUM_ = s(APPIUM, _COLON_);
    /** Constant representing the message when actual values are not valid. */
    public static final String ACTUAL_VALUES_ARE_NOT_VALID = b(c(ACTUAL), VALUES, ARE, NOT, VALID);
    /** Constant representing the message when adjacent log records are too similar with the same text. */
    public static final String ADJACENT___ = b(c(ADJACENT), LOG, RECORDS, ARE, TOO, SIMILAR, WITH, SAME, TEXT);
    /** Constant representing the message when allowed colours amount is redefined in. */
    public static final String ALLOWED___ = b(c(ALLOWED), COLOURS, AMOUNT, IS, REDEFINED, IN);
    /** Constant representing the phrase 'amount of'. */
    public static final String AMOUNT_OF = b(AMOUNT, OF);
    /** Constant representing the phrase 'Android launcher'. */
    public static final String ANDROID_LAUNCHER = b(c(ANDROID), c(LAUNCHER));
    /** Constant representing the message when an element could not be located on the page using the given search parameters. */
    public static final String AN_ELEMENT___ = b(c(AN), ELEMENT, COULD, NOT, BE, LOCATED, ON, THE, PAGE, USING, THE, GIVEN,
            SEARCH, s(PARAMETERS, _DOT_));

    /** Constant representing Appium automation name capability. */
    public static final String APPIUM_AUTOMATION_NAME = s(APPIUM_, AUTOMATION, c(NAME));
    /** Constant representing Appium app package capability. */
    public static final String APPIUM_APP_PACKAGE = s(APPIUM_, APP, c(PACKAGE));
    /** Constant representing Appium app activity capability. */
    public static final String APPIUM_APP_ACTIVITY = s(APPIUM_, APP, c(ACTIVITY));
    /** Constant representing Appium no reset capability. */
    public static final String APPIUM_NO_RESET = s(APPIUM_, NO, c(RESET));
    /** Constant representing Appium UDID capability. */
    public static final String APPIUM_UDID = s(APPIUM_, UD, ID);
    /** Constant representing Appium log. */
    public static final String APPIUM_LOG = s(APPIUM, _DASH_, LOG);
    /** Constant representing the phrase 'are too similar'. */
    public static final String ARE_TOO_SIMILAR = b(ARE, TOO, SIMILAR);
    /** Constant representing the phrase 'asserting that actual'. */
    public static final String ASSERTING_THAT_ACTUAL = b(c(ASSERTING), THAT, ACTUAL);
    /** Constant representing the phrase 'asserting that value'. */
    public static final String ASSERTING_THAT_VALUE = b(c(ASSERTING), THAT, VALUE);
    /** Constant representing the message for attempting counter action. */
    public static final String ATTEMPTING_COUNTER_ACTION = b(c(ATTEMPTING), d(COUNTER, ACTION));
}
