package link.lang;

import static link.lang.word.L.LOGIN;
import static link.lang.word.L.LOGOUT;
import static works.lysenko.util.data.strs.Bind.b;
import static works.lysenko.util.data.strs.Case.c;
import static works.lysenko.util.lang.word.E.EXPECTED;
import static works.lysenko.util.lang.word.M.MESSAGE;
import static works.lysenko.util.lang.word.S.SUCCESSFUL;
import static works.lysenko.util.lang.word.V.VERIFYING;

@SuppressWarnings({"ClassWithoutLogger", "MissingJavadoc", "StaticMethodOnlyUsedInOneClass", "AutoBoxing", "WeakerAccess"})
public record V() {

    public static final String VERIFYING_EXPECTED_MESSAGE = b(c(VERIFYING), EXPECTED, MESSAGE);
    public static final String VERIFYING_SUCCESSFUL_LOGIN = b(c(VERIFYING), SUCCESSFUL, LOGIN);
    public static final String VERIFYING_SUCCESSFUL_LOGOUT = b(c(VERIFYING), SUCCESSFUL, LOGOUT);
}
