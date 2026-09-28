package works.lysenko.util.apis.execution;

import works.lysenko.base.Logs;
import works.lysenko.util.apis.log._LogRecord;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Represents an abstraction for managing issues and associated log records.
 * This interface defines methods for managing and querying known issues,
 * latest log records, and unreproduced issues.
 */
@SuppressWarnings("InterfaceWithOnlyOneDirectInheritor")
public interface _Issues {

    /**
     * @param logRecord log record to add
     */
    void add(_LogRecord logRecord);

    /**
     * Used by {@link Logs}
     *
     * @param p string to search Known Issues
     * @return Set of KnownIssues for a given query
     */
    Set<String> getKnownIssueFor(String p);

    /**
     * Retrieves the collection of known issues.
     *
     * @return The collection of known issues as a {@code Collection} of {@code String}.
     */
    Collection<String> known();

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

    /**
     * @return the set of strings representing issues that have not been reproduced
     */
    Set<String> notReproduced();
}
