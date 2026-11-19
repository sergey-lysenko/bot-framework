package works.lysenko.util.prop.core;

import static works.lysenko.util.prop.PropEnum._SCREEN_TO_SPAWN_DASHBOARD;
import static works.lysenko.util.prop.PropEnum._TEST_APP;
import static works.lysenko.util.prop.PropEnum._TEST_BUNDLE_ID;
import static works.lysenko.util.prop.PropEnum._WEBD_FORCED_PLATFORM;

/**
 * Startup and environment configurations for test runs.
 */
@SuppressWarnings({"StaticNonFinalField", "StaticMethodOnlyUsedInOneClass"})
public record Start() {

    /**
     * Target mobile application file path or name (or empty string if not configured).
     */
    public static String app = _TEST_APP.get();

    /**
     * Target application bundle identifier (e.g. for mobile testing).
     */
    public static String bundleId = _TEST_BUNDLE_ID.get();

    /**
     * Override platform identifier to force (or {@code null} if default detection is used).
     */
    public static String forcedPlatform = _WEBD_FORCED_PLATFORM.get();

    /**
     * Screen/monitor index on which to spawn the dashboard UI.
     */
    public static int dashboardScreen = _SCREEN_TO_SPAWN_DASHBOARD.get();

    /**
     * Refreshes static fields from current test properties.
     */
    public static void refresh() {
        app = _TEST_APP.get();
        bundleId = _TEST_BUNDLE_ID.get();
        forcedPlatform = _WEBD_FORCED_PLATFORM.get();
        dashboardScreen = _SCREEN_TO_SPAWN_DASHBOARD.get();
    }
}
