package works.lysenko.util.prop.core;

import static works.lysenko.util.spec.PropEnum._BUNDLE_ID;
import static works.lysenko.util.spec.PropEnum._FORCED_PLATFORM;
import static works.lysenko.util.spec.PropEnum._SCREEN_TO_SPAWN_DASHBOARD;

/**
 * Represents a class to manage start properties for the application.
 * <p>
 * Provides information about the bundle ID, forced platform, location, and dashboard screen.
 */
@SuppressWarnings({"MissingJavadoc", "StaticMethodOnlyUsedInOneClass", "NonFinalStaticVariableUsedInClassInitialization"})
public record Start() {

    public static final String bundleId = _BUNDLE_ID.get();
    public static final String forcedPlatform = _FORCED_PLATFORM.get(); // Silent as null is passable
    public static final int dashboardScreen = _SCREEN_TO_SPAWN_DASHBOARD.get();
}
