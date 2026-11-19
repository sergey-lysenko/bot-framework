package works.lysenko.util.apis.core;

import works.lysenko.util.apis.data._Result;
import works.lysenko.util.apis.log._LogRecord;
import works.lysenko.util.apis.scenario._Scenario;

import java.util.List;
import java.util.ServiceLoader;

/**
 * Service-provider bridge decoupling {@code framework-core} event/result classification from {@code framework-analysis}.
 */
public interface _DiagnosticsProvider {

    /**
     * Checks whether the given event record matches a configured expected response and should be neutralized.
     *
     * @param scenario  associated scenario (may be null)
     * @param result    associated result (may be null)
     * @param logRecord event log record
     * @return true if the event is expected and neutralized
     */
    boolean isExpectedEvent(_Scenario scenario, _Result result, _LogRecord logRecord);

    /**
     * Checks whether the given event record matches a known issue configured to decrease failures to warnings.
     *
     * @param scenario  associated scenario (may be null for root events)
     * @param logRecord event log record
     * @return true if severity should be decreased to warning
     */
    boolean shouldDecreaseToWarning(_Scenario scenario, _LogRecord logRecord);

    /**
     * Saves run-scoped known-issues and expected-responses snapshot files.
     */
    void saveRunSnapshots();

    /**
     * Discovers all registered {@link _DiagnosticsProvider} implementations on the classpath.
     *
     * @return immutable list of providers
     */
    static List<_DiagnosticsProvider> providers() {
        return Holder.PROVIDERS;
    }

    /**
     * Queries active providers whether the event is expected.
     *
     * @param scenario  associated scenario (may be null)
     * @param result    associated result (may be null)
     * @param logRecord event log record
     * @return true if any provider classifies the event as expected
     */
    static boolean checkExpectedEvent(final _Scenario scenario, final _Result result, final _LogRecord logRecord) {
        for (final _DiagnosticsProvider p : Holder.PROVIDERS) {
            if (p.isExpectedEvent(scenario, result, logRecord)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Queries active providers whether failure severity should be decreased to warning.
     *
     * @param scenario  associated scenario (may be null)
     * @param logRecord event log record
     * @return true if any provider requests decreasing to warning
     */
    static boolean checkDecreaseToWarning(final _Scenario scenario, final _LogRecord logRecord) {
        for (final _DiagnosticsProvider p : Holder.PROVIDERS) {
            if (p.shouldDecreaseToWarning(scenario, logRecord)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Dispatches {@link #saveRunSnapshots()} to all active providers.
     */
    static void fireSaveRunSnapshots() {
        for (final _DiagnosticsProvider p : Holder.PROVIDERS) {
            p.saveRunSnapshots();
        }
    }

    /**
     * Lazy holder for classpath-discovered providers.
     */
    final class Holder {
        static final List<_DiagnosticsProvider> PROVIDERS =
                ServiceLoader.load(_DiagnosticsProvider.class).stream()
                        .map(ServiceLoader.Provider::get)
                        .toList();

        private Holder() {
        }
    }
}
