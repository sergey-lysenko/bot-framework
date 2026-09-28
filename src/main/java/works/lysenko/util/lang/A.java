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


@SuppressWarnings({"ClassWithoutLogger", "MissingJavadoc", "StaticMethodOnlyUsedInOneClass", "AutoBoxing"})
public record A() {

    private static final String APPIUM_ = s(APPIUM, _COLON_);
    public static final String ACTUAL_VALUES_ARE_NOT_VALID = b(c(ACTUAL), VALUES, ARE, NOT, VALID);
    public static final String ADJACENT___ = b(c(ADJACENT), LOG, RECORDS, ARE, TOO, SIMILAR, WITH, SAME, TEXT);
    public static final String ALLOWED___ = b(c(ALLOWED), COLOURS, AMOUNT, IS, REDEFINED, IN);
    public static final String AMOUNT_OF = b(AMOUNT, OF);
    public static final String ANDROID_LAUNCHER = b(c(ANDROID), c(LAUNCHER));
    public static final String AN_ELEMENT___ = b(c(AN), ELEMENT, COULD, NOT, BE, LOCATED, ON, THE, PAGE, USING, THE, GIVEN,
            SEARCH, s(PARAMETERS, _DOT_));

    public static final String APPIUM_AUTOMATION_NAME = s(APPIUM_, AUTOMATION, c(NAME));
    public static final String APPIUM_APP_PACKAGE = s(APPIUM_, APP, c(PACKAGE));
    public static final String APPIUM_APP_ACTIVITY = s(APPIUM_, APP, c(ACTIVITY));
    public static final String APPIUM_NO_RESET = s(APPIUM_, NO, c(RESET));
    public static final String APPIUM_UDID = s(APPIUM_, UD, ID);
    public static final String APPIUM_LOG = s(APPIUM, _DASH_, LOG);
    public static final String ARE_TOO_SIMILAR = b(ARE, TOO, SIMILAR);
    public static final String ASSERTING_THAT_ACTUAL = b(c(ASSERTING), THAT, ACTUAL);
    public static final String ASSERTING_THAT_VALUE = b(c(ASSERTING), THAT, VALUE);
    public static final String ATTEMPTING_COUNTER_ACTION = b(c(ATTEMPTING), d(COUNTER, ACTION));
}
