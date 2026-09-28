package works.lysenko.util.lang;

import static works.lysenko.util.chrs.___.ORG;
import static works.lysenko.util.data.strs.Bind.b;
import static works.lysenko.util.data.strs.Case.c;
import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.lang.word.E.EXPECTATIONS;
import static works.lysenko.util.lang.word.O.OPENQA;
import static works.lysenko.util.lang.word.O.ORIGINAL;
import static works.lysenko.util.lang.word.P.PERSISTED;
import static works.lysenko.util.lang.word.R.REMOTE;
import static works.lysenko.util.lang.word.S.SELENIUM;
import static works.lysenko.util.spec.Symbols._COLON_;
import static works.lysenko.util.spec.Symbols._DOT_;

@SuppressWarnings({"MissingJavadoc", "StaticMethodOnlyUsedInOneClass"})
public record O() {

    public static final String OOS = b(_DOT_, ORG, OPENQA, SELENIUM);
    public static final String OOSR = b(_DOT_, OOS, REMOTE);
    public static final String ORIGINAL_EXPECTATIONS_PERSISTED = b(c(ORIGINAL), EXPECTATIONS, s(PERSISTED, _COLON_));
}
