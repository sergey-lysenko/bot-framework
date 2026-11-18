package works.lysenko.util.prop.data;

import static works.lysenko.util.prop.PropEnum._DATA_SNAPSHOTS_ALLOWED;
import static works.lysenko.util.prop.PropEnum._DATA_SNAPSHOTS_FULL;

/**
 * Configuration for execution data snapshots.
 * <p>
 * Controls whether data snapshot capture is permitted and whether full snapshot content
 * is retrieved from the execution environment.
 */
public record Snapshot() {

    /**
     * Indicates whether capturing data snapshots is permitted.
     */
    public static final boolean allowed = _DATA_SNAPSHOTS_ALLOWED.get();

    /**
     * Indicates whether full data snapshot contents should be captured.
     */
    public static final boolean full = _DATA_SNAPSHOTS_FULL.get();
}
