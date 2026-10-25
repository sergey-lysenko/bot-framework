package works.lysenko.shared.lang.word;

import static works.lysenko.shared.lang.chrs.__.GN;
import static works.lysenko.util.chrs.__.*;
import static works.lysenko.util.chrs.___.*;
import static works.lysenko.util.data.strs.Swap.*;
import static works.lysenko.util.spec.Symbols.*;

/**
 * Shared language word string constants starting with 'S'.
 */
@SuppressWarnings({"ClassWithoutLogger", "StaticMethodOnlyUsedInOneClass", "AutoBoxing", "WeakerAccess"})
public record S() {

    /**
     * String constant for "sign".
     */
    public static final String SIGN = s(SI, GN);

    /**
     * String constant for "signing".
     */
    public static final String SIGNING = s(SIGN, ING);

    /**
     * String constant for "settings".
     */
    public static final String SETTINGS = s(SE, TT, ING, S);

    /**
     * String constant for "save".
     */
    public static final String SAVE = s(SA, VE);
}
