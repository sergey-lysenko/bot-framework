package works.lysenko.shared.lang;

import static works.lysenko.shared.lang.S.SIGN_IN;
import static works.lysenko.shared.lang.word.P.PASSWORD;
import static works.lysenko.util.chrs.__.*;
import static works.lysenko.util.chrs.___.*;
import static works.lysenko.util.chrs.____.*;
import static works.lysenko.util.data.strs.Bind.*;
import static works.lysenko.util.data.strs.Case.*;
import static works.lysenko.util.data.strs.Swap.*;
import static works.lysenko.util.lang.word.A.*;
import static works.lysenko.util.lang.word.C.*;
import static works.lysenko.util.lang.word.W.*;
import static works.lysenko.util.spec.Symbols.*;

/**
 * Shared language string constants starting with 'A'.
 */
@SuppressWarnings({"ClassWithoutLogger", "StaticMethodOnlyUsedInOneClass", "AutoBoxing", "WeakerAccess"})
public record A() {

    /**
     * String constant for "Attempting to sign in with".
     */
    public static final String ATTEMPTING_TO_SIGN_IN_WITH = b(c(ATTEMPTING), TO, SIGN_IN, WITH);

    /**
     * String constant for "and wrong password".
     */
    public static final String AND_WRONG_PASSWORD = b(AND, WRONG, PASSWORD);

    /**
     * String constant for "and correct password".
     */
    public static final String AND_CORRECT_PASSWORD = b(AND, CORRECT, PASSWORD);
}
