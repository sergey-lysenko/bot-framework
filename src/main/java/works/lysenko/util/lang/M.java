package works.lysenko.util.lang;

import static works.lysenko.util.chrs.____.*;
import static works.lysenko.util.chrs._____.VALUE;
import static works.lysenko.util.data.enums.Brackets.ROUND;
import static works.lysenko.util.data.strs.Bind.b;
import static works.lysenko.util.data.strs.Case.c;
import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.data.strs.Wrap.e;
import static works.lysenko.util.lang.word.A.ACTIVITY;
import static works.lysenko.util.lang.word.A.ACTUAL;
import static works.lysenko.util.lang.word.A.AMOUNT;
import static works.lysenko.util.lang.word.E.EMPTY;
import static works.lysenko.util.lang.word.F.FAILURE;
import static works.lysenko.util.lang.word.I.IMMINENT;
import static works.lysenko.util.lang.word.M.*;
import static works.lysenko.util.lang.word.P.PERFORMED;
import static works.lysenko.util.lang.word.R.*;
import static works.lysenko.util.lang.word.S.SCREENSHOT;
import static works.lysenko.util.spec.Symbols._COMMA_;

@SuppressWarnings({"ClassWithoutLogger", "MissingJavadoc", "StaticMethodOnlyUsedInOneClass", "AutoBoxing"})
public record M() {

    public static final String MAIN_ACTIVITY = s(c(MAIN), c(ACTIVITY));
    public static final String MAKING_SCREENSHOT = b(c(MAKING), SCREENSHOT);
    public static final String MANUALLY_REQUESTED_TEST_STOP_PERFORMED = b(c(MANUALLY), REQUESTED, c(TEST), STOP, PERFORMED);
    public static final String MAXIMUM_RETRIES_AMOUNT___ = b(c(MAXIMUM), RETRIES, AMOUNT, s(REACHED, _COMMA_), s(FIND,
            e(ROUND, EMPTY)), RETURNS, s(NULL, _COMMA_), TEST, FAILURE, IMMINENT);
    public static final String MERGING_ACTUAL_VALUE = b(c(MERGING), ACTUAL, VALUE);
}
