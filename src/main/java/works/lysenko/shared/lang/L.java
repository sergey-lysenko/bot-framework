package works.lysenko.shared.lang;

import static works.lysenko.shared.lang.word.L.LOGGING;
import static works.lysenko.util.chrs.___.OUT;
import static works.lysenko.util.data.strs.Bind.b;
import static works.lysenko.util.data.strs.Case.c;

/**
 * Shared language string constants starting with 'L'.
 */
@SuppressWarnings({"ClassWithoutLogger", "StaticMethodOnlyUsedInOneClass", "AutoBoxing", "WeakerAccess"})
public record L() {

    /**
     * String constant for "Logging out".
     */
    public static final String LOGGING_OUT = b(c(LOGGING), OUT);
}
