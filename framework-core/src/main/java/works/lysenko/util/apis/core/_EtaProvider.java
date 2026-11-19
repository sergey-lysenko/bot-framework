package works.lysenko.util.apis.core;

import works.lysenko.util.apis.scenario._Scenario;

import java.util.List;
import java.util.ServiceLoader;

/**
 * Service-provider bridge decoupling {@code framework-core} execution lifecycle from {@code framework-analysis}.
 */
public interface _EtaProvider {

    /**
     * Resets run-scoped ETA and leaf-completion state at the start of a new results session.
     */
    void onReset();

    /**
     * Invoked when a leaf or mono scenario cycle begins.
     *
     * @param shortName scenario short name
     */
    void onCycleStart(String shortName);

    /**
     * Invoked when a leaf or mono scenario cycle ends.
     *
     * @param shortName     scenario short name
     * @param elapsedMs     elapsed run milliseconds
     * @param completedLeaf completed leaf/mono scenario
     */
    void onCycleEnd(String shortName, long elapsedMs, _Scenario completedLeaf);

    /**
     * Invoked during cycle limbo to record cycle telemetry if no leaf completed.
     *
     * @param testNumber current test cycle number
     * @param elapsedMs  elapsed run milliseconds
     */
    void onLimboCycle(Integer testNumber, long elapsedMs);

    /**
     * Invoked when the test run finishes to persist historical ETA calibration data.
     *
     * @param totalRuntimeMs total run duration in milliseconds
     */
    void onRunCompletion(long totalRuntimeMs);

    /**
     * Captures a live ETA telemetry JSON payload for {@code dash/eta}.
     *
     * @param markDone  whether to mark the snapshot as completed
     * @param nowMs     current epoch milliseconds
     * @param elapsedMs elapsed run milliseconds
     * @param title     current run title
     * @return serialized JSON string, or {@code null}
     */
    String captureEtaJson(boolean markDone, long nowMs, long elapsedMs, String title);

    /**
     * Discovers all registered {@link _EtaProvider} implementations on the classpath.
     *
     * @return immutable list of providers
     */
    static List<_EtaProvider> providers() {
        return Holder.PROVIDERS;
    }

    /**
     * Dispatches {@link #onReset()} to all active providers.
     */
    static void fireReset() {
        for (final _EtaProvider p : Holder.PROVIDERS) {
            p.onReset();
        }
    }

    /**
     * Dispatches {@link #onCycleStart(String)} to all active providers.
     *
     * @param shortName scenario short name
     */
    static void fireCycleStart(final String shortName) {
        for (final _EtaProvider p : Holder.PROVIDERS) {
            p.onCycleStart(shortName);
        }
    }

    /**
     * Dispatches {@link #onCycleEnd(String, long, _Scenario)} to all active providers.
     *
     * @param shortName     scenario short name
     * @param elapsedMs     elapsed run milliseconds
     * @param completedLeaf completed leaf/mono scenario
     */
    static void fireCycleEnd(final String shortName, final long elapsedMs, final _Scenario completedLeaf) {
        for (final _EtaProvider p : Holder.PROVIDERS) {
            p.onCycleEnd(shortName, elapsedMs, completedLeaf);
        }
    }

    /**
     * Dispatches {@link #onLimboCycle(Integer, long)} to all active providers.
     *
     * @param testNumber current test cycle number
     * @param elapsedMs  elapsed run milliseconds
     */
    static void fireLimboCycle(final Integer testNumber, final long elapsedMs) {
        for (final _EtaProvider p : Holder.PROVIDERS) {
            p.onLimboCycle(testNumber, elapsedMs);
        }
    }

    /**
     * Dispatches {@link #onRunCompletion(long)} to all active providers.
     *
     * @param totalRuntimeMs total run duration in milliseconds
     */
    static void fireRunCompletion(final long totalRuntimeMs) {
        for (final _EtaProvider p : Holder.PROVIDERS) {
            p.onRunCompletion(totalRuntimeMs);
        }
    }

    /**
     * Resolves a serialized {@code dash/eta} JSON payload from the active provider, or {@code null} if none is present.
     *
     * @param markDone  whether to mark the snapshot as completed
     * @param nowMs     current epoch milliseconds
     * @param elapsedMs elapsed run milliseconds
     * @param title     current run title
     * @return JSON payload or {@code null}
     */
    static String resolveEtaJson(final boolean markDone, final long nowMs, final long elapsedMs, final String title) {
        if (!Holder.PROVIDERS.isEmpty()) {
            return Holder.PROVIDERS.get(0).captureEtaJson(markDone, nowMs, elapsedMs, title);
        }
        return null;
    }

    /**
     * Lazy holder for classpath-discovered providers.
     */
    final class Holder {
        static final List<_EtaProvider> PROVIDERS =
                ServiceLoader.load(_EtaProvider.class).stream()
                        .map(ServiceLoader.Provider::get)
                        .toList();

        private Holder() {
        }
    }
}
