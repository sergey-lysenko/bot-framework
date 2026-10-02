package link.lang;

import static link.lang.word.L.LOGGING;
import static works.lysenko.util.chrs.___.OUT;
import static works.lysenko.util.data.strs.Bind.b;
import static works.lysenko.util.data.strs.Case.c;

@SuppressWarnings({"ClassWithoutLogger", "MissingJavadoc", "StaticMethodOnlyUsedInOneClass", "AutoBoxing", "WeakerAccess"})
public record L() {

    public static final String LOGGING_OUT = b(c(LOGGING), OUT);
}
