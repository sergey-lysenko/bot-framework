package works.lysenko.util.lang;

import static works.lysenko.util.chrs.__.BY;
import static works.lysenko.util.chrs.__.OF;
import static works.lysenko.util.chrs.___.WAS;
import static works.lysenko.util.chrs.____.CODE;
import static works.lysenko.util.chrs.____.PAGE;
import static works.lysenko.util.data.strs.Bind.b;
import static works.lysenko.util.data.strs.Case.c;
import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.lang.D.DUE_TO;
import static works.lysenko.util.lang.word.C.CONFIGURATION;
import static works.lysenko.util.lang.word.D.DISABLED;
import static works.lysenko.util.lang.word.E.ELEMENT;
import static works.lysenko.util.lang.word.E.EXCEPTION;
import static works.lysenko.util.lang.word.E.EXECUTION;
import static works.lysenko.util.lang.word.F.FAILED;
import static works.lysenko.util.lang.word.R.REFERENCE;
import static works.lysenko.util.lang.word.R.REQUEST;
import static works.lysenko.util.lang.word.S.*;
import static works.lysenko.util.spec.Symbols._COLON_;

@SuppressWarnings({"ClassWithoutLogger", "MissingJavadoc", "StaticMethodOnlyUsedInOneClass", "AutoBoxing", "unused"})
public record S() {

    public static final String SCENARIOS_STACK = b(c(SCENARIOS), s(STACK, _COLON_));
    public static final String SCENARIO_EXECUTION_FAILED_DUE_TO = b(c(SCENARIO), EXECUTION, FAILED, DUE_TO);
    public static final String SHRINKING_WAS_DISABLED_BY_CONFIGURATION = b(c(SHRINKING), WAS, DISABLED, BY, CONFIGURATION);
    public static final String SHRINKING_WAS_DISABLED_BY_REQUEST = b(c(SHRINKING), WAS, DISABLED, BY, REQUEST);
    public static final String SNAPSHOT_OF_PAGE_CODE = b(SNAPSHOT, OF, PAGE, CODE);
    public static final String S_E_R_EXCEPTION = s(c(STALE), c(ELEMENT), c(REFERENCE), c(EXCEPTION));
}
