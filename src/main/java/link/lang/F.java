package link.lang;

import static link.lang.word.F.FILLING;
import static works.lysenko.util.chrs.__.IN;
import static works.lysenko.util.chrs.___.FOR;
import static works.lysenko.util.chrs.____.DATA;
import static works.lysenko.util.data.strs.Bind.b;
import static works.lysenko.util.data.strs.Case.c;

@SuppressWarnings({"ClassWithoutLogger", "MissingJavadoc", "StaticMethodOnlyUsedInOneClass", "AutoBoxing", "WeakerAccess"})
public record F() {

    public static final String FILLING_IN_DATA_FOR = b(c(FILLING), IN, DATA, FOR);
}
