package works.lysenko.base.output;

import works.lysenko.base.issues.KnownIssuesLoader;
import works.lysenko.base.responses.ExpectedResponsesLoader;
import works.lysenko.util.apis.core._ReportingProvider;
import works.lysenko.util.apis.core._Reports;

import java.io.File;
import java.util.List;

import static works.lysenko.Base.section;
import static works.lysenko.util.lang.word.T.TREE;

/**
 * Default {@link _ReportingProvider} implementation registered via {@code META-INF/services}.
 */
public final class DefaultReportingProvider implements _ReportingProvider {

    private static final _Reports REPORTS = new Reports();

    @Override
    public void onInitialize() {
        ProgressionSettings.initialize();
    }

    @Override
    public List<String> treeSonificationValues() {
        return ProgressionSettings.treeSonificationValues();
    }

    @Override
    public void onNode() {
        TreeTracker.onNode();
    }

    @Override
    public void onLimbo(final Integer testNumber) {
        ScenarioTracker.onLimbo(testNumber);
        TreeTracker.onLimbo(testNumber);
    }

    @Override
    public void onRunStats() {
        REPORTS.run();
        KnownIssuesLoader.saveRunKnownIssues();
        ExpectedResponsesLoader.saveRunExpectedResponses();
        section(TREE);
        TreeHtml.treeStats();
        LogHtml.logStats();
    }

    @Override
    public void onComplete() {
        ScenarioTracker.onComplete();
        TreeTracker.onComplete();
    }

    @Override
    public void launchDetachedPostProcessor(
            final File logFile,
            final File htmlFile,
            final boolean openBrowser,
            final boolean showProgressWindow) {
        PostProcessor.launchDetached(logFile, htmlFile, openBrowser, showProgressWindow);
    }
}
