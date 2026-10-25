package works.lysenko.util.lang;

import static works.lysenko.util.chrs.__.*;
import static works.lysenko.util.chrs.___.*;
import static works.lysenko.util.chrs.____.*;
import static works.lysenko.util.chrs._____.THERE;
import static works.lysenko.util.data.strs.Bind.b;
import static works.lysenko.util.data.strs.Case.c;
import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.lang.word.A.ABSENT;
import static works.lysenko.util.lang.word.C.COMPLETED;
import static works.lysenko.util.lang.word.D.DEFINED;
import static works.lysenko.util.lang.word.D.DIFFERS;
import static works.lysenko.util.lang.word.E.EXCEPTION;
import static works.lysenko.util.lang.word.E.EXECUTION;
import static works.lysenko.util.lang.word.E.EXPECTED;
import static works.lysenko.util.lang.word.H.HAPPEN;
import static works.lysenko.util.lang.word.I.INITIALISED;
import static works.lysenko.util.lang.word.P.PREEMPTIVELY;
import static works.lysenko.util.lang.word.P.PROPERTIES;
import static works.lysenko.util.lang.word.R.RANGES;
import static works.lysenko.util.lang.word.S.*;
import static works.lysenko.util.lang.word.T.*;
import static works.lysenko.util.lang.word.W.WRITER;
import static works.lysenko.util.spec.Symbols._COLON_;
import static works.lysenko.util.spec.Symbols._DOT_;

/**
 * Language utility constants starting with T.
 */
@SuppressWarnings({"ClassWithoutLogger", "StaticMethodOnlyUsedInOneClass", "unused"})
public record T() {

    /** Constant representing telemetry writer is not initialised message. */
    public static final String TELEMETRY_WRITER_IS_NOT___ = b(s(c(TELEMETRY), c(WRITER)), IS, NOT, INITIALISED);
    /** Constant representing tests had been stopped preemptively message. */
    public static final String TESTS_HAD_BEEN_STOPPED_PREEMPTIVELY = b(c(TESTS), HAD, BEEN, STOPPED, PREEMPTIVELY);
    /** Constant representing test execution. */
    public static final String TEST_EXECUTION = b(TEST, EXECUTION);
    /** Constant representing test properties are absent. */
    public static final String TEST_PROPERTIES_ARE___ = b(c(TEST), PROPERTIES, ARE, s(ABSENT, _DOT_));
    /** Constant representing test session completed. */
    public static final String TEST_SESSION_COMPLETED = b(c(TEST), SESSION, COMPLETED);
    /** Constant representing text in. */
    public static final String TEXT_IN = b(c(TEXT), IN);
    /** Constant representing there are. */
    public static final String THERE_ARE = b(c(THERE), ARE);
    /** Constant representing the sorted list of ranges differs by. */
    public static final String THE_SORTED___ = b(c(THE), SORTED, LIST, OF, RANGES, DIFFERS, BY);
    /** Constant representing this should not happen message. */
    public static final String THIS_SHOULD_NOT_HAPPEN = b(c(THIS), SHOULD, NOT, HAPPEN);
    /** Constant representing threshold defined for. */
    public static final String THRESHOLD_DEFINED_IN = b(THRESHOLD, DEFINED, FOR);
    /** Constant representing thumbnail for. */
    public static final String THUMBNAIL_FOR = b(THUMBNAIL, FOR);
    /** Constant representing timeout exception name. */
    public static final String TIMEOUT_EXCEPTION = s(c(TIMEOUT), c(EXCEPTION));
    /** Constant representing TODO prefix. */
    public static final String TODO_ = s(TO, DO, _COLON_);
    /** Constant representing too few expected. */
    public static final String TOO_FEW_EXPECTED = b(TOO, FEW, EXPECTED);
    /** Constant representing the phrase 'to be'. */
    public static final String TO_BE = b(TO, BE);
    /** Constant representing tried for 30 seconds wait description. */
    public static final String TRIED_FOR_30_SECONDS___ = " (tried for 30 second(s) with 500 milliseconds interval)"; //NON-NLS
}
