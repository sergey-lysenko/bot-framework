package works.lysenko.util.lang;

import static works.lysenko.util.data.strs.Bind.b;
import static works.lysenko.util.data.strs.Case.c;
import static works.lysenko.util.lang.word.E.EXCEPTIONAL;
import static works.lysenko.util.lang.word.L.LOCATOR;
import static works.lysenko.util.lang.word.P.PROCESSING;
import static works.lysenko.util.lang.word.S.STATE;

/**
 * Language utility constants starting with P.
 */
@SuppressWarnings({"ClassWithoutLogger", "StaticMethodOnlyUsedInOneClass", "AutoBoxing"})
public record P() {

    /** Constant representing processing exceptional state. */
    public static final String PROCESSING_EXCEPTIONAL_STATE = b(c(PROCESSING), EXCEPTIONAL, STATE);
    /** Constant representing provided locator. */
    public static final String PROVIDED_LOCATOR = b(c(works.lysenko.util.lang.word.P.PROVIDED), LOCATOR);
}
