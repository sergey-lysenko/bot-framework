package works.lysenko.base.logger.renderer;

import works.lysenko.util.apis.log._LogRecord;

/**
 * Interface for rendering LogRecords to a specific format.
 */
public interface LogRenderer {
    
    /**
     * Render the log record.
     * 
     * @param record the log record
     * @param totalTests total tests count
     * @param spanLength span length for padding
     * @param previousId previous log record id
     * @return rendered string
     */
    String render(_LogRecord record, Integer totalTests, int spanLength, Long previousId);
}
