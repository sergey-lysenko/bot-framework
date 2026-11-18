package works.lysenko.util.prop.logs;

import works.lysenko.util.data.enums.Severity;

import static works.lysenko.util.prop.PropEnum._TEST_LONG_OPERATION_SEVERITY;
import static works.lysenko.util.prop.PropEnum._TEST_LONG_OPERATION_THRESHOLD;

/**
 * Configuration for identifying and logging long operations during execution.
 * <p>
 * Provides threshold and severity settings to trigger events when the duration
 * between log records exceeds expected operation time limits.
 */
public record LongOperation() {

    /**
     * Duration threshold (in milliseconds) above which an operation interval is considered excessive.
     */
    public static final int threshold = _TEST_LONG_OPERATION_THRESHOLD.get();

    /**
     * Severity level assigned to log events emitted when an operation exceeds the duration threshold.
     */
    public static final Severity severity = _TEST_LONG_OPERATION_SEVERITY.get();
}
