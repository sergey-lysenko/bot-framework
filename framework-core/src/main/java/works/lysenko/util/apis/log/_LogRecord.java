package works.lysenko.util.apis.log;

import works.lysenko.util.apis.scenario._Scenario;

/**
 * Log Records Interface
 */
@SuppressWarnings("InterfaceWithOnlyOneDirectInheritor")
public interface _LogRecord {

    /**
     * Retrieves the log data associated with this log record.
     *
     * @return log data
     */
    _LogData data();

    /**
     * Retrieves the exact scenario instance active when this log record was emitted, if any.
     *
     * @return emitting scenario instance, or {@code null} when emitted outside a scenario
     */
    default _Scenario scenario() {
        return null;
    }

    /**
     * Retrieves the logical execution context name for this log record
     * (scenario FQN when inside a scenario, or lifecycle phase such as {@code "Pretest"}, {@code "Preflight"},
     * {@code "Postflight"} when outside any scenario).
     *
     * @return execution context name, or {@code null} if unspecified
     */
    default String context() {
        final _Scenario s = scenario();
        return null != s ? s.getName() : null;
    }

    /**
     * Calculates and sets the time span since current time.
     *
     * @param currentTime for defining span
     * @return calculated span
     */
    int setSpan(long currentTime);

    /**
     * Retrieves the calculated time span.
     *
     * @return span
     */
    int span();

    /**
     * Retrieves the test index associated with this log record.
     *
     * @return test index of the LogRecord
     */
    int test();

    /**
     * Retrieves the text of this log record.
     *
     * @return text of the LogRecord
     */
    String text();

    /**
     * Retrieves the timestamp of this log record.
     *
     * @return timestamp of the LogRecord
     */
    Long time();
}
