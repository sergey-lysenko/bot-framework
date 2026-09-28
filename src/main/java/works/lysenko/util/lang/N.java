package works.lysenko.util.lang;

import static works.lysenko.util.chrs.__.*;
import static works.lysenko.util.chrs.___.*;
import static works.lysenko.util.chrs.____.NULL;
import static works.lysenko.util.chrs.____.WERE;
import static works.lysenko.util.chrs.____.WITH;
import static works.lysenko.util.chrs._____.VALUE;
import static works.lysenko.util.data.strs.Bind.b;
import static works.lysenko.util.data.strs.Case.c;
import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.lang.word.A.ACCEPTABLE;
import static works.lysenko.util.lang.word.A.ACTUAL;
import static works.lysenko.util.lang.word.A.ALLOWED;
import static works.lysenko.util.lang.word.C.COLOURS;
import static works.lysenko.util.lang.word.E.*;
import static works.lysenko.util.lang.word.F.FOUND;
import static works.lysenko.util.lang.word.M.MARGINS;
import static works.lysenko.util.lang.word.N.NEITHER;
import static works.lysenko.util.lang.word.N.NOTHING;
import static works.lysenko.util.lang.word.P.PERFORMED;
import static works.lysenko.util.lang.word.P.PROPERTY;
import static works.lysenko.util.lang.word.R.REQUESTED;
import static works.lysenko.util.lang.word.T.TESTS;
import static works.lysenko.util.spec.Symbols._COLON_;
import static works.lysenko.util.spec.Symbols._EQUAL_;
import static works.lysenko.util.spec.Symbols._EXCL_;

@SuppressWarnings({"MissingJavadoc", "StaticMethodOnlyUsedInOneClass", "ClassWithoutLogger", "unused"})
public record N() {

    public static final String NEW_COLOURS_PROPERTY___ = b(c(NEW), COLOURS, PROPERTY, VALUE, WITH, EXTENDED, s(MARGINS,
            _COLON_));
    public static final String NOT_ENOUGH = b(c(NOT), ENOUGH);
    public static final String NOT_EQUAL_TO_EXPECTED = b(NOT, EQUAL, TO, EXPECTED);
    public static final String NOT_FOUND = b(NOT, FOUND);
    public static final String NULL_IS_NOT_ACCEPTABLE = b(c(NULL), IS, NOT, ACCEPTABLE);
    public static final String NOTHING_TO_DO_NOW = b(NOTHING, TO, DO, NOW);
    public static final String NO_TESTS_WERE_PERFORMED = b(c(NO), TESTS, WERE, PERFORMED);
    public static final String NOT_EQUAL = s(_EXCL_, _EQUAL_);
    public static final String NEITHER_ACTUAL_NOR___ = b(c(NEITHER), ACTUAL, NOR, REQUESTED, VALUE, ALLOWED, TO, BE, NULL);
}
