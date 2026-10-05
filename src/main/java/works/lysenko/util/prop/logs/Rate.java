package works.lysenko.util.prop.logs;

import works.lysenko.util.data.enums.Severity;

import static works.lysenko.util.spec.PropEnum.*;

@SuppressWarnings({"NonFinalStaticVariableUsedInClassInitialization", "StaticMethodOnlyUsedInOneClass", "MissingJavadoc"
})
public record Rate() {

    public static final int red = _LOGS_RATE_4.get();
    public static final int yellow = _LOGS_RATE_3.get();
    public static final int green = _LOGS_RATE_2.get();
    public static final int blue = _LOGS_RATE_1.get();
}
