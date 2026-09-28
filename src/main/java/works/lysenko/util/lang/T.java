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

@SuppressWarnings({"ClassWithoutLogger", "MissingJavadoc", "StaticMethodOnlyUsedInOneClass", "unused"})
public record T() {

    public static final String TELEMETRY_WRITER_IS_NOT___ = b(s(c(TELEMETRY), c(WRITER)), IS, NOT, INITIALISED);
    public static final String TESTS_HAD_BEEN_STOPPED_PREEMPTIVELY = b(c(TESTS), HAD, BEEN, STOPPED, PREEMPTIVELY);
    public static final String TEST_EXECUTION = b(TEST, EXECUTION);
    public static final String TEST_PROPERTIES_ARE___ = b(c(TEST), PROPERTIES, ARE, s(ABSENT, _DOT_));
    public static final String TEST_SESSION_COMPLETED = b(c(TEST), SESSION, COMPLETED);
    public static final String TEXT_IN = b(c(TEXT), IN);
    public static final String THERE_ARE = b(c(THERE), ARE);
    public static final String THE_SORTED___ = b(c(THE), SORTED, LIST, OF, RANGES, DIFFERS, BY);
    public static final String THIS_SHOULD_NOT_HAPPEN = b(c(THIS), SHOULD, NOT, HAPPEN);
    public static final String THRESHOLD_DEFINED_IN = b(THRESHOLD, DEFINED, FOR);
    public static final String THUMBNAIL_FOR = b(THUMBNAIL, FOR);
    public static final String TIMEOUT_EXCEPTION = s(c(TIMEOUT), c(EXCEPTION));
    public static final String TODO_ = s(TO, DO, _COLON_);
    public static final String TOO_FEW_EXPECTED = b(TOO, FEW, EXPECTED);
    public static final String TO_BE = b(TO, BE);
    public static final String TRIED_FOR_30_SECONDS___ = " (tried for 30 second(s) with 500 milliseconds interval)"; //NON-NLS
}
