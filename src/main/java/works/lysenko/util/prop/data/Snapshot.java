package works.lysenko.util.prop.data;

import static works.lysenko.util.spec.PropEnum._DATA_SNAPSHOTS_ALLOWED;
import static works.lysenko.util.spec.PropEnum._DATA_SNAPSHOTS_FULL;

/**
 * A1 class that contains static properties related to snapshot settings.
 * <p>
 * Provides the following static boolean properties:
 * - allowed: Indicates if data snapshot is allowed.
 * - full: Indicates if full data snapshot is allowed.
 * <p>
 * This class interacts with a properties instance to retrieve boolean values based on specific keys.
 * The values for allowed and full are obtained using the properties instance.
 */
@SuppressWarnings({"MissingJavadoc", "NonFinalStaticVariableUsedInClassInitialization", "StaticMethodOnlyUsedInOneClass"})
public record Snapshot() {

    public static final boolean allowed = _DATA_SNAPSHOTS_ALLOWED.get();
    public static final boolean full = _DATA_SNAPSHOTS_FULL.get();
}
