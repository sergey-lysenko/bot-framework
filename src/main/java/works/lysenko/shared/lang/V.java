package works.lysenko.shared.lang;

import static works.lysenko.shared.lang.word.L.LOGIN;
import static works.lysenko.shared.lang.word.L.LOGOUT;
import static works.lysenko.util.data.strs.Bind.b;
import static works.lysenko.util.data.strs.Case.c;
import static works.lysenko.util.lang.word.E.EXPECTED;
import static works.lysenko.util.lang.word.M.MESSAGE;
import static works.lysenko.util.lang.word.S.SUCCESSFUL;
import static works.lysenko.util.lang.word.V.VERIFYING;

/**
 * Shared language string constants starting with 'V'.
 */
@SuppressWarnings({"ClassWithoutLogger", "StaticMethodOnlyUsedInOneClass", "AutoBoxing", "WeakerAccess"})
public record V() {

    /**
     * String constant for "Verifying expected message".
     */
    public static final String VERIFYING_EXPECTED_MESSAGE = b(c(VERIFYING), EXPECTED, MESSAGE);

    /**
     * String constant for "Verifying successful login".
     */
    public static final String VERIFYING_SUCCESSFUL_LOGIN = b(c(VERIFYING), SUCCESSFUL, LOGIN);

    /**
     * String constant for "Verifying successful logout".
     */
    public static final String VERIFYING_SUCCESSFUL_LOGOUT = b(c(VERIFYING), SUCCESSFUL, LOGOUT);
}
