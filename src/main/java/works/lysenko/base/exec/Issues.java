package works.lysenko.base.exec;

import works.lysenko.util.apis.execution._Issues;
import works.lysenko.util.apis.log._LogRecord;
import works.lysenko.util.spec.Layout;

import java.util.*;
import java.util.function.BiConsumer;

/**
 * The {@code Issues} class implements the {@link _Issues} interface and is responsible
 * for managing a collection of known issues, the latest log records, and issues
 * that have not been reproduced. It provides methods to handle and query issues
 * based on log records and predefined criteria.
 */
public class Issues implements _Issues {

    private final Collection<String> known = new HashSet<>(0);
    private final List<_LogRecord> latest = new ArrayList<>(0);
    private final Set<String> notReproduced = null;

    public final Collection<String> known() {

        return known;
    }

    public final List<_LogRecord> latest() {

        return latest;
    }

    public final Set<String> notReproduced() {

        return notReproduced;
    }

    public final void add(final _LogRecord logRecord) {

        latest.add(logRecord);
    }


    public final Set<String> getKnownIssueFor(final String p) {

        final Set<String> ki = new HashSet<>(0);
        Layout.Files.knownIssues.forEach(new Ki(p, ki));
        return ki;
    }

    public final boolean isLatestEmpty() {

        return latest().isEmpty();
    }

    public final List<_LogRecord> latestCopy() {

        return new ArrayList<>(latest());
    }

    @SuppressWarnings("StandardVariableNames")
    private class Ki implements BiConsumer<Object, Object> {

        private final String p;
        private final Set<? super String> ki;

        Ki(final String p, final Set<? super String> ki) {

            this.p = p;
            this.ki = ki;
        }

        @SuppressWarnings("DataFlowIssue")
        @Override
        public final void accept(final Object k, final Object v) {

            // Adds key when value matches; reconciles known/reproduced status
            if (p.contains((CharSequence) v)) {
                ki.add((String) k);
                if (notReproduced().contains(k)) {
                    notReproduced().remove(k);
                    known().add((String) k);
                }
            }
        }
    }
}
