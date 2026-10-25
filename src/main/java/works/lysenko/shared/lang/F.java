package works.lysenko.shared.lang;

import static works.lysenko.shared.lang.word.F.FILLING;
import static works.lysenko.util.chrs.__.IN;
import static works.lysenko.util.chrs.___.FOR;
import static works.lysenko.util.chrs.____.DATA;
import static works.lysenko.util.data.strs.Bind.b;
import static works.lysenko.util.data.strs.Case.c;

/**
 * Shared language string constants starting with 'F'.
 */
@SuppressWarnings({"ClassWithoutLogger", "StaticMethodOnlyUsedInOneClass", "AutoBoxing", "WeakerAccess"})
public record F() {

    /**
     * String constant for "Filling in data for".
     */
    public static final String FILLING_IN_DATA_FOR = b(c(FILLING), IN, DATA, FOR);
}
