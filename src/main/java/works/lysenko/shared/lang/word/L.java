package works.lysenko.shared.lang.word;

import static works.lysenko.util.chrs.__.*;
import static works.lysenko.util.chrs.___.*;
import static works.lysenko.util.data.strs.Swap.*;
import static works.lysenko.util.spec.Symbols.*;

/**
 * Shared language word string constants starting with 'L'.
 */
@SuppressWarnings({"ClassWithoutLogger", "StaticMethodOnlyUsedInOneClass", "AutoBoxing", "WeakerAccess"})
public record L() {

    /**
     * String constant for "logging".
     */
    public static final String LOGGING = s(LO, GG, ING);

    /**
     * String constant for "login".
     */
    public static final String LOGIN = s(LOG, IN);

    /**
     * String constant for "logout".
     */
    public static final String LOGOUT = s(LOG, OUT);

    /**
     * String constant for "logged".
     */
    public static final String LOGGED = s(LOG, G, ED);

    /**
     * String constant for "logs".
     */
    public static final String LOGS = s(LOG, S);
}
