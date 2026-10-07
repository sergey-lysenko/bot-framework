package works.lysenko.util.data.enums;

/**
 * First-class Execution Status representing the outcome of a scenario or test.
 */
public enum ExecutionStatus {

    /**
     * Not yet visited/executed.
     */
    UNVISITED("unvisited", "#64748b"),

    /**
     * Executed without warnings or failures.
     */
    PASSED("passed", "#22c55e"),

    /**
     * Executed with warnings or non-failing notices.
     */
    WARNING("warning", "#f59e0b"),

    /**
     * Executed and encountered a failure or unhandled exception.
     */
    FAILED("failed", "#ef4444"),

    /**
     * Executed successfully, but an underlying child scenario encountered a failure.
     */
    CHILD_FAILED("child_failed", "#fbbf24");

    private final String code;
    private final String colorHex;

    ExecutionStatus(final String code, final String colorHex) {

        this.code = code;
        this.colorHex = colorHex;
    }

    /**
     * @return the status code string
     */
    public String code() {

        return code;
    }

    /**
     * @return the status color hex code
     */
    public String colorHex() {

        return colorHex;
    }
}
