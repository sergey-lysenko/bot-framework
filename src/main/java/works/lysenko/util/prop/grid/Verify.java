package works.lysenko.util.prop.grid;

import static works.lysenko.util.spec.PropEnum.*;

@SuppressWarnings({"StaticMethodOnlyUsedInOneClass", "MissingJavadoc"})
public record Verify() {

    public static final boolean colours = _GRID_VERIFY_COLOURS.get();
}
