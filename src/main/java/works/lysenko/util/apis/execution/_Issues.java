package works.lysenko.util.apis.execution;

import works.lysenko.util.apis.log._LogRecord;

import java.util.List;

/**
 * Represents an abstraction for managing issues and associated log records.
 */
@SuppressWarnings("InterfaceWithOnlyOneDirectInheritor")
public interface _Issues {

    /**
     * @param logRecord log record to add
     */
    void add(_LogRecord logRecord);

    /**
     * @return new issues list
     */
    List<_LogRecord> latest();

    /**
     * @return new issues list (copy)
     */
    List<_LogRecord> latestCopy();

    /**
     * @return true if empty
     */
    boolean isLatestEmpty();
}
