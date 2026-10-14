package works.lysenko.util.prop.data;

import static works.lysenko.util.prop.PropEnum._DATA_PERSIST;

/**
 * Configuration for data persistence settings.
 */
public record Persist() {

    /**
     * Indicates whether data snapshots and execution data should be persisted to disk.
     */
    public static final Boolean data = _DATA_PERSIST.get();
}
