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

    public final List<_LogRecord> latest() {

        return latest;
    }

    public final void add(final _LogRecord logRecord) {

        latest.add(logRecord);
    }

    public final boolean isLatestEmpty() {

        return latest().isEmpty();
    }

    public final List<_LogRecord> latestCopy() {

        return new ArrayList<>(latest());
    }
}
