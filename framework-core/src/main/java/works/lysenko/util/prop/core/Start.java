package works.lysenko.util.prop.core;

import static works.lysenko.util.prop.PropEnum._SCREEN_TO_SPAWN_DASHBOARD;
import static works.lysenko.util.prop.PropEnum._TEST_BUNDLE_ID;
import static works.lysenko.util.prop.PropEnum._WEBD_FORCED_PLATFORM;

/**
 * Startup and environment configurations for test runs.
 */
public record Start() {

    /**
     * Target application bundle identifier (e.g. for mobile testing).
     */
    public static final String bundleId = _TEST_BUNDLE_ID.get();

    /**
     * Override platform identifier to force (or {@code null} if default detection is used).
     */
    public static final String forcedPlatform = _WEBD_FORCED_PLATFORM.get();

    /**
     * Screen/monitor index on which to spawn the dashboard UI.
     */
    public static final int dashboardScreen = _SCREEN_TO_SPAWN_DASHBOARD.get();
}
