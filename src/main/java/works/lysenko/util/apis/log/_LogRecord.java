package works.lysenko.util.apis.log;

/**
 * Log Records Interface
 */
@SuppressWarnings("InterfaceWithOnlyOneDirectInheritor")
public interface _LogRecord {

    /**
     * @return data
     */
    _LogData data();

    /**
     * @param totalTests total tests of test session
     * @param spanLength required length of span section
     * @param previousId
     * @return rendered LogRecord
     */
    String render(Integer totalTests, int spanLength, Long previousId);

    /**
     * @param totalTests for definition of width
     * @return rendered Test
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
     * @param currentTime for defining span
     * @return calculated span
     */
    int setSpan(long currentTime);

    /**
     * @return test of the LogRecord
     */
    int test();

    /**
     * @return text of the LogRecord
     */
    String text();

    /**
     * @return timestamp of the LogRecord
     */
    Long time();
}
