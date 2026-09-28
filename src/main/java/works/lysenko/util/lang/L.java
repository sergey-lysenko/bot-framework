package works.lysenko.util.lang;

import static works.lysenko.util.chrs.__.*;
import static works.lysenko.util.chrs.___.*;
import static works.lysenko.util.data.strs.Bind.b;
import static works.lysenko.util.data.strs.Case.c;
import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.lang.word.I.INITIALISED;
import static works.lysenko.util.lang.word.U.UNABLE;
import static works.lysenko.util.lang.word.W.WRITE;
import static works.lysenko.util.lang.word.W.WRITER;
import static works.lysenko.util.spec.Symbols.*;

@SuppressWarnings({"ClassWithoutLogger", "MissingJavadoc", "StaticMethodOnlyUsedInOneClass", "AutoBoxing"})
public record L() {

    public static final String LOG_WRITER_IS_NOT___ = b(s(c(LOG), c(WRITER)), IS, NOT, s(INITIALISED, _COMMA_), UNABLE, TO, WRITE, LOG);
}
