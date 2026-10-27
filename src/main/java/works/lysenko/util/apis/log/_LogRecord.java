package works.lysenko.util.apis.log;

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
