package works.lysenko.util.prop.core;

import static works.lysenko.util.spec.PropEnum._SCREEN_TO_SPAWN_DASHBOARD;
import static works.lysenko.util.spec.PropEnum._TEST_BUNDLE_ID;
import static works.lysenko.util.spec.PropEnum._WEBD_FORCED_PLATFORM;

/**
 * Represents a class to manage start properties for the application.
 * <p>
 * Provides information about the bundle ID, forced platform, location, and dashboard screen.
 */
@SuppressWarnings({"MissingJavadoc", "StaticMethodOnlyUsedInOneClass", "NonFinalStaticVariableUsedInClassInitialization"})
public record Start() {

    public static final String bundleId = _TEST_BUNDLE_ID.get();
    public static final String forcedPlatform = _WEBD_FORCED_PLATFORM.get(); // Silent as null is passable
    public static final int dashboardScreen = _SCREEN_TO_SPAWN_DASHBOARD.get();
}
