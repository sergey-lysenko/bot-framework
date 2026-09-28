package works.lysenko.util.lang;

import static works.lysenko.util.data.strs.Bind.b;
import static works.lysenko.util.lang.word.F.FULLSCREEN;
import static works.lysenko.util.lang.word.I.IMAGE;

@SuppressWarnings("MissingJavadoc")
public record F() {

    static final String FULLSCREEN_IMAGE = b(FULLSCREEN, IMAGE);
}
