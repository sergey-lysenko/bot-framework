package works.lysenko.base.output;

/**
 * Deprecated placeholder after removing redundant run.json generation in favor of run.log + sidecars.
 */
@Deprecated
public final class Json {

    private Json() {
    }

    /**
     * No-op; run.json generation has been removed.
     */
    public static void jsonStats() {
        // Intentionally empty: run.log and sidecar files are the single source of truth.
    }
}
