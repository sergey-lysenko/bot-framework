package link.lang;

import static link.lang.word.B.BOX;
import static works.lysenko.util.chrs.___.THE;
import static works.lysenko.util.data.strs.Bind.b;
import static works.lysenko.util.data.strs.Case.c;
import static works.lysenko.util.lang.word.C.CLOSING;
import static works.lysenko.util.lang.word.D.DIALOGUE;

@SuppressWarnings({"ClassWithoutLogger", "MissingJavadoc", "StaticMethodOnlyUsedInOneClass", "AutoBoxing", "WeakerAccess"})
public record C() {

    public static final String CLOSING_THE_DIALOGUE_BOX = b(c(CLOSING), THE, DIALOGUE, BOX);
}
