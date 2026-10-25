package works.lysenko.shared.lang;

import static works.lysenko.shared.lang.word.I.*;
import static works.lysenko.util.data.strs.Bind.*;
import static works.lysenko.util.data.strs.Case.*;
import static works.lysenko.util.lang.word.E.*;
import static works.lysenko.util.lang.word.M.*;

/**
 * Shared language string constants starting with 'I'.
 */
@SuppressWarnings({"ClassWithoutLogger", "StaticMethodOnlyUsedInOneClass", "AutoBoxing", "WeakerAccess"})
public record I() {

    /**
     * String constant for "Improper error message".
     */
    public static final String IMPROPER_ERROR_MESSAGE = b(c(IMPROPER), ERROR, MESSAGE);
}
