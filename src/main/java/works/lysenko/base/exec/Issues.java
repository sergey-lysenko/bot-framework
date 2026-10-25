package works.lysenko.base.exec;

import works.lysenko.util.apis.execution._Issues;
import works.lysenko.util.apis.log._LogRecord;

import java.util.ArrayList;
import java.util.List;

/**
 * The {@code Issues} class implements the {@link _Issues} interface and is responsible
 * for managing the latest log records.
 */
public class Issues implements _Issues {

    private final List<_LogRecord> latest = new ArrayList<>(0);

    /**
     * Returns the list of latest log records.
     *
     * @return list of latest log records
     */
    public final List<_LogRecord> latest() {

        return latest;
    }

    /**
     * Adds a log record to the issues list.
     *
     * @param logRecord log record to add
     */
    public final void add(final _LogRecord logRecord) {

        latest.add(logRecord);
    }

    /**
     * Checks if the latest log records list is empty.
     *
     * @return true if empty, false otherwise
     */
    public final boolean isLatestEmpty() {

        return latest().isEmpty();
    }

    /**
     * Returns a shallow copy of the latest log records list.
     *
     * @return copy of latest log records list
     */
    public final List<_LogRecord> latestCopy() {

        return new ArrayList<>(latest());
    }
}
