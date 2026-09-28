package works.lysenko.util.lang;

import static works.lysenko.util.data.strs.Bind.b;
import static works.lysenko.util.data.strs.Case.c;
import static works.lysenko.util.lang.word.I.ISSUES;
import static works.lysenko.util.lang.word.K.KNOWN;

@SuppressWarnings({"ClassWithoutLogger", "MissingJavadoc", "StaticMethodOnlyUsedInOneClass"})
public record K() {

    public static final String KNOWN_ISSUES = b(c(KNOWN), c(ISSUES));
}
