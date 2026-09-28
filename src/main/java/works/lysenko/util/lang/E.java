package works.lysenko.util.lang;

import static io.opentelemetry.semconv.SemanticAttributes.JvmThreadStateValues.BLOCKED;
import static works.lysenko.util.chrs.__.*;
import static works.lysenko.util.chrs.___.*;
import static works.lysenko.util.chrs.____.GRID;
import static works.lysenko.util.chrs.____.NULL;
import static works.lysenko.util.chrs.____.PATH;
import static works.lysenko.util.data.strs.Bind.b;
import static works.lysenko.util.data.strs.Case.c;
import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.lang.word.C.CONDITION;
import static works.lysenko.util.lang.word.C.CONFIGURATION;
import static works.lysenko.util.lang.word.C.CONFIGURED;
import static works.lysenko.util.lang.word.D.DEFINITION;
import static works.lysenko.util.lang.word.E.*;
import static works.lysenko.util.lang.word.F.FAILED;
import static works.lysenko.util.lang.word.L.LOCATED;
import static works.lysenko.util.lang.word.R.REQUESTED;
import static works.lysenko.util.lang.word.S.SCENARIO;
import static works.lysenko.util.lang.word.U.UPDATE;
import static works.lysenko.util.lang.word.V.VISIBILITY;
import static works.lysenko.util.lang.word.W.WAITING;
import static works.lysenko.util.spec.Symbols.*;

@SuppressWarnings({"ClassWithoutLogger", "MissingJavadoc", "StaticMethodOnlyUsedInOneClass"})
public record E() {

    public static final String ELEMENT_IS_NULL = b(ELEMENT, IS, NULL);
    public static final String EQUALS_TO_EXPECTED = b(EQUALS, TO, EXPECTED);
    public static final String EXPECTATIONS_UPDATE_REQUESTED_BY___ = b(EXPECTATIONS, UPDATE, WAS, REQUESTED, BY,
            CONFIGURATION, BUT, BLOCKED, BY, GRID, DEFINITION);
    public static final String EXPECTATIONS_UPDATE_WAS_NOT___ = b(EXPECTATIONS, UPDATE, WAS, NOT, CONFIGURED);
    public static final String EXPECTED_CONDITION_FAILED___ = b(c(EXPECTED), CONDITION, s(FAILED, _COLON_), WAITING, FOR,
            VISIBILITY, OF, ELEMENT, LOCATED, BY, s(c(BY), _DOT_, X, PATH, _COLON_, _SPACE_));
    public static final String EXCEPTIONAL_SCENARION = b(c(EXCEPTIONAL), c(SCENARIO));
}
