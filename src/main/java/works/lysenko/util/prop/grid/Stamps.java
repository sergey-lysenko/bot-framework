package works.lysenko.util.prop.grid;

import static works.lysenko.util.spec.PropEnum.*;

@SuppressWarnings({"StaticMethodOnlyUsedInOneClass", "MissingJavadoc"})
public record Stamps() {

    public static final boolean process = _STAMPS_PROCESS.get();
    public static final boolean display = _STAMPS_DISPLAY.get();
    public static final boolean compress = _STAMPS_COMPRESS.get();

}
