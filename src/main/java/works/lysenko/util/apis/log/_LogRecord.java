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
     * Renders the log record as a formatted string.
     *
     * @param totalTests total tests of test session
     * @param spanLength required length of span section
     * @param previousId identifier of the previous record
     * @return rendered LogRecord
     */
    String render(Integer totalTests, int spanLength, Long previousId);

    /**
     * Renders the test identifier with defined width.
     *
     * @param totalTests total tests for definition of width
     * @return rendered Test string
     */
    String renderTest(Integer totalTests);

    /**
     * Renders and retrieves the unique identifier associated with the LogRecord.
     *
     * @return a string representation of the rendered identifier
     */
    String renderId();

    /**
     * Render the time with given width.
     *
     * @param width the width of the time
     * @return the rendered time
     */
    String renderTime(int width);

    /**
     * Calculates and sets the time span since current time.
     *
     * @param currentTime for defining span
     * @return calculated span
     */
    int setSpan(long currentTime);

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
