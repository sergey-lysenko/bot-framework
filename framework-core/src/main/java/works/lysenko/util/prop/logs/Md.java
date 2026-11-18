package works.lysenko.util.prop.logs;

import static works.lysenko.util.prop.PropEnum.*;

@SuppressWarnings({"StaticMethodOnlyUsedInOneClass", "AutoBoxing"})
public record Md() {

    public static final boolean timestamps = _LOGS_MD_TIMESTAMPS.get();
    public static final boolean durations = _LOGS_MD_DURATIONS.get();
}
