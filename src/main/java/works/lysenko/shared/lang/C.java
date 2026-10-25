package works.lysenko.shared.lang;

import static works.lysenko.shared.lang.word.B.BOX;
import static works.lysenko.util.chrs.___.THE;
import static works.lysenko.util.data.strs.Bind.b;
import static works.lysenko.util.data.strs.Case.c;
import static works.lysenko.util.lang.word.C.CLOSING;
import static works.lysenko.util.lang.word.D.DIALOGUE;

/**
 * Shared language string constants starting with 'C'.
 */
@SuppressWarnings({"ClassWithoutLogger", "StaticMethodOnlyUsedInOneClass", "AutoBoxing", "WeakerAccess"})
public record C() {

    /**
     * String constant for "Closing the dialogue box".
     */
    public static final String CLOSING_THE_DIALOGUE_BOX = b(c(CLOSING), THE, DIALOGUE, BOX);
}
