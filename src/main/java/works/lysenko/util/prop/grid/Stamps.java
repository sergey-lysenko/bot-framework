package works.lysenko.util.prop.grid;

import static works.lysenko.util.spec.PropEnum.*;

@SuppressWarnings({"StaticMethodOnlyUsedInOneClass", "MissingJavadoc"})
public record Stamps() {

    public static final boolean process = _GRID_STAMPS_PROCESS.get();
    public static final boolean display = _GRID_STAMPS_DISPLAY.get();
    public static final boolean compress = _GRID_STAMPS_COMPRESS.get();

}
