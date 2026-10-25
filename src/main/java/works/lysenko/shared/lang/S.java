package works.lysenko.shared.lang;

import static works.lysenko.shared.lang.word.S.*;
import static works.lysenko.util.chrs.__.*;
import static works.lysenko.util.data.strs.Bind.*;

/**
 * Shared language string constants starting with 'S'.
 */
@SuppressWarnings({"ClassWithoutLogger", "StaticMethodOnlyUsedInOneClass", "AutoBoxing", "WeakerAccess"})
public record S() {

    /**
     * String constant for "sign in".
     */
    public static final String SIGN_IN = b(SIGN, IN);
}
