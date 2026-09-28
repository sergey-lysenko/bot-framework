package works.lysenko.util.lang;

import static works.lysenko.util.chrs.__.IS;
import static works.lysenko.util.chrs.__.OF;
import static works.lysenko.util.chrs.___.ARE;
import static works.lysenko.util.chrs.___.ONE;
import static works.lysenko.util.chrs.___.TOO;
import static works.lysenko.util.chrs.____.*;
import static works.lysenko.util.data.strs.Bind.b;
import static works.lysenko.util.data.strs.Case.c;
import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.lang.F.FULLSCREEN_IMAGE;
import static works.lysenko.util.lang.word.D.DENSE;
import static works.lysenko.util.lang.word.G.GETTING;
import static works.lysenko.util.lang.word.P.PARTIAL;
import static works.lysenko.util.lang.word.P.PIXELS;
import static works.lysenko.util.lang.word.S.SAMPLED;
import static works.lysenko.util.lang.word.S.SCREENSHOT;
import static works.lysenko.util.spec.Symbols._COMMA_;

@SuppressWarnings({"ClassWithoutLogger", "MissingJavadoc", "StaticMethodOnlyUsedInOneClass", "AutoBoxing"})
public record G() {

    public static final String GETTING_FULLSCREEN_IMAGE = b(c(GETTING), FULLSCREEN_IMAGE);
    public static final String GRID_IS_TOO_DENSE___ = b(c(GRID), IS, TOO, s(DENSE, _COMMA_), SOME, PIXELS, ARE, SAMPLED,
            MORE, THEN, ONE, TIME);
    public static final String GETTING_PARTIAL_SCREENSHOT_OF = b(c(GETTING), PARTIAL, SCREENSHOT, OF);
}
