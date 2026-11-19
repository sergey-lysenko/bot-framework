package works.lysenko.base.issues;

import works.lysenko.base.responses.ExpectedResponsesLoader;
import works.lysenko.base.responses.ExpectedResponsesStore;
import works.lysenko.util.apis.core._DiagnosticsProvider;
import works.lysenko.util.apis.data._Result;
import works.lysenko.util.apis.log._LogRecord;
import works.lysenko.util.apis.scenario._Scenario;

import java.util.Collections;
import java.util.List;

/**
 * Default {@link _DiagnosticsProvider} implementation backed by {@link KnownIssuesStore} and {@link ExpectedResponsesStore}.
 */
public final class DefaultDiagnosticsProvider implements _DiagnosticsProvider {

    @Override
    public boolean isExpectedEvent(final _Scenario scenario, final _Result result, final _LogRecord logRecord) {
        if (null == logRecord || null == scenario) return false;
        final ExpectedResponsesStore expStore = ExpectedResponsesStore.getGlobalStore();
        if (null == expStore) return false;
        if (expStore.isExpected(scenario, logRecord)) {
            return true;
        }
        final String text = logRecord.text();
        return null != text && text.contains("[BROWSER]") && null != result && expStore.matchesNode(scenario, result);
    }

    @Override
    public boolean shouldDecreaseToWarning(final _Scenario scenario, final _LogRecord logRecord) {
        if (null == logRecord) return false;
        final KnownIssuesStore kiStore = KnownIssuesStore.getGlobalStore();
        if (null == kiStore) return false;
        final List<KnownIssue> matches = (null != scenario)
                ? kiStore.getForLogRecord(scenario, logRecord)
                : kiStore.getForLogRecord(Collections.emptyList(), logRecord);
        for (final KnownIssue ki : matches) {
            if (ki.decreaseToWarning()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void saveRunSnapshots() {
        KnownIssuesLoader.saveRunKnownIssues();
        ExpectedResponsesLoader.saveRunExpectedResponses();
    }
}
