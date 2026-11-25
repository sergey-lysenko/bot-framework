package works.lysenko.util.apis.core;

import java.io.File;
import java.util.List;
import java.util.ServiceLoader;

/**
 * Service-provider bridge decoupling {@code framework-core} execution lifecycle from {@code framework-reporting}.
 */
public interface _ReportingProvider {

    /**
     * Initializes run-scoped reporting and progression settings after properties are loaded.
     */
    void onInitialize();

    /**
     * Returns all accepted values for the tree sonification property.
     *
     * @return accepted sonification mode names
     */
    List<String> treeSonificationValues();

    /**
     * Returns the default tree sonification mode names for random selection.
     *
     * @return default sonification mode names
     */
    default List<String> defaultTreeSonifications() {
        return List.of();
    }

    /**
     * Invoked when a scenario node starts execution.
     */
    void onNode();

    /**
     * Invoked between test cycles (limbo) to capture progression frames.
     *
     * @param testNumber current test cycle number
     */
    void onLimbo(Integer testNumber);

    /**
     * Invoked when run summary statistics and report tables are generated.
     */
    void onRunStats();

    /**
     * Invoked during engine finalization to flush asynchronous progression frames.
     */
    void onComplete();

    /**
     * Launches detached post-processing of logs, HTML reports, and animations.
     *
     * @param logFile             source run log file
     * @param htmlFile            target HTML report file
     * @param openBrowser         whether to open the generated HTML report in the browser
     * @param showProgressWindow  whether to display the Swing progress window
     */
    void launchDetachedPostProcessor(File logFile, File htmlFile, boolean openBrowser, boolean showProgressWindow);

    /**
     * Discovers all registered {@link _ReportingProvider} implementations on the classpath.
     *
     * @return immutable list of providers
     */
    static List<_ReportingProvider> providers() {
        return Holder.PROVIDERS;
    }

    /**
     * Dispatches {@link #onInitialize()} to all active providers.
     */
    static void fireInitialize() {
        for (final _ReportingProvider p : Holder.PROVIDERS) {
            p.onInitialize();
        }
    }

    /**
     * Resolves accepted tree sonification mode values from the active reporting provider.
     *
     * @return list of valid sonification mode strings
     */
    static List<String> resolveTreeSonificationValues() {
        if (!Holder.PROVIDERS.isEmpty()) {
            return Holder.PROVIDERS.getFirst().treeSonificationValues();
        }
        return List.of("none");
    }

    /**
     * Resolves default tree sonification mode names from the active reporting provider.
     *
     * @return list of default sonification mode strings, or empty list when no reporting provider is present
     */
    static List<String> resolveDefaultTreeSonifications() {
        if (!Holder.PROVIDERS.isEmpty()) {
            return Holder.PROVIDERS.getFirst().defaultTreeSonifications();
        }
        return List.of();
    }

    /**
     * Dispatches {@link #onNode()} to all active providers.
     */
    static void fireOnNode() {
        for (final _ReportingProvider p : Holder.PROVIDERS) {
            p.onNode();
        }
    }

    /**
     * Dispatches {@link #onLimbo(Integer)} to all active providers.
     *
     * @param testNumber current test cycle number
     */
    static void fireOnLimbo(final Integer testNumber) {
        for (final _ReportingProvider p : Holder.PROVIDERS) {
            p.onLimbo(testNumber);
        }
    }

    /**
     * Dispatches {@link #onRunStats()} to all active providers.
     */
    static void fireOnRunStats() {
        for (final _ReportingProvider p : Holder.PROVIDERS) {
            p.onRunStats();
        }
    }

    /**
     * Dispatches {@link #onComplete()} to all active providers.
     */
    static void fireOnComplete() {
        for (final _ReportingProvider p : Holder.PROVIDERS) {
            p.onComplete();
        }
    }

    /**
     * Dispatches {@link #launchDetachedPostProcessor(File, File, boolean, boolean)} to all active providers.
     *
     * @param logFile            source run log file
     * @param htmlFile           target HTML report file
     * @param openBrowser        whether to open browser
     * @param showProgressWindow whether to show progress window
     */
    static void fireLaunchDetached(
            final File logFile,
            final File htmlFile,
            final boolean openBrowser,
            final boolean showProgressWindow) {
        for (final _ReportingProvider p : Holder.PROVIDERS) {
            p.launchDetachedPostProcessor(logFile, htmlFile, openBrowser, showProgressWindow);
        }
    }

    /**
     * Lazy holder for classpath-discovered providers.
     */
    final class Holder {
        static final List<_ReportingProvider> PROVIDERS =
                ServiceLoader.load(_ReportingProvider.class).stream()
                        .map(ServiceLoader.Provider::get)
                        .toList();

        private Holder() {
        }
    }
}
