package works.lysenko.util.prop.tree;

import static works.lysenko.util.spec.PropEnum._INCLUDE_DOWNSTREAM;
import static works.lysenko.util.spec.PropEnum._INCLUDE_UPSTREAM;

/**
 * Represents an Include class which provides static boolean properties
 * for enabling upstream and downstream inclusions.
 * <p>
 * The class includes the following static properties:
 * - upstream: Indicates whether upstream inclusion is enabled.
 * - downstream: Indicates whether downstream inclusion is enabled.
 * <p>
 * This class interacts with a properties instance to retrieve boolean
 * values based on predefined keys.
 */
@SuppressWarnings({"MissingJavadoc", "NonFinalStaticVariableUsedInClassInitialization"})
public record Include() {

    public static Boolean upstream = _INCLUDE_UPSTREAM.get();
    public static Boolean downstream = _INCLUDE_DOWNSTREAM.get();

    /**
     * Refreshes upstream and downstream flags from current test properties.
     */
    public static void refresh() {

        upstream = _INCLUDE_UPSTREAM.get();
        downstream = _INCLUDE_DOWNSTREAM.get();
    }
}
