package works.lysenko.util.lang;

import static works.lysenko.util.data.strs.Bind.b;
import static works.lysenko.util.lang.word.F.FULLSCREEN;
import static works.lysenko.util.lang.word.I.IMAGE;

/**
 * Language utility constants starting with F.
 */
public record F() {

    /** Constant representing full screen image. */
    public static final String FULLSCREEN_IMAGE = b(FULLSCREEN, IMAGE);
}
