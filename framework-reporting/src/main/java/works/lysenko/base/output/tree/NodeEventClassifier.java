package works.lysenko.base.output.tree;

import works.lysenko.base.issues.KnownIssue;
import works.lysenko.base.issues.KnownIssuesMatcher;
import works.lysenko.base.output.TreeHtml;
import works.lysenko.base.output.TreeHtml.NodeData;
import works.lysenko.base.responses.ExpectedResponse;
import works.lysenko.base.responses.ExpectedResponsesMatcher;
import works.lysenko.base.responses.ExpectedResponsesStore;
import works.lysenko.util.apis.data._Event;
import works.lysenko.util.apis.data._Result;
import works.lysenko.util.apis.log._LogRecord;
import works.lysenko.util.apis.scenario._Scenario;
import works.lysenko.util.data.enums.ExecutionStatus;
import works.lysenko.util.data.enums.Severity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Single-pass, incrementally cached event classifier for {@link NodeData}.
 * <p>
 * Computes failure, warning, notice, and unclassified event count in one pass over a node's
 * append-only {@link _Result#getEvents()} list, caching progress per {@link _Result} (weakly)
 * so repeated checks within a frame and across frames scale with newly added events rather than
 * total accumulated run events.
 */
public final class NodeEventClassifier {

    /**
     * Immutable classification result for a tree node at the current instant.
     */
    public record Classification(
            boolean hasFailure,
            boolean hasWarning,
            boolean hasNotice,
            int unclassifiedEventCount) {

        static final Classification EMPTY = new Classification(false, false, false, 0);
    }

    private static final class ResultCursor {
        final ExpectedResponsesStore expStore;
        final int responsesSize;
        final String nodeKey;
        final List<KnownIssue> knownIssuesSnapshot;
        final List<ExpectedResponse> matchingResponses;
        final boolean hasBlankExpectedPattern;

        int scannedUpTo;
        _LogRecord firstRecord;
        _LogRecord lastRecord;

        boolean nodeMatchedExpected;
        int nonBrowserCount;
        int browserWithDataCount;
        boolean nonBrowserFailure;
        boolean browserFailure;
        boolean nonBrowserWarning;
        boolean browserWarning;
        boolean nonBrowserNotice;
        boolean browserNotice;

        ResultCursor(
                final ExpectedResponsesStore expStore,
                final int responsesSize,
                final String nodeKey,
                final List<KnownIssue> knownIssuesSnapshot,
                final List<ExpectedResponse> matchingResponses,
                final boolean hasBlankExpectedPattern) {
            this.expStore = expStore;
            this.responsesSize = responsesSize;
            this.nodeKey = nodeKey;
            this.knownIssuesSnapshot = knownIssuesSnapshot;
            this.matchingResponses = matchingResponses;
            this.hasBlankExpectedPattern = hasBlankExpectedPattern;
            this.nodeMatchedExpected = hasBlankExpectedPattern;
        }
    }

    private static final Object CURSOR_LOCK = new Object();
    private static final Map<_Result, ResultCursor> CURSORS = new WeakHashMap<>();

    private NodeEventClassifier() {
    }

    /**
     * Classifies a node's current failure, warning, notice, and unclassified event status.
     *
     * @param node tree node to classify
     * @return current {@link Classification}
     */
    public static Classification classify(final NodeData node) {
        if (null == node) return Classification.EMPTY;
        final Classification precomputed = node.precomputedClassification();
        if (null != precomputed) return precomputed;

        final _Scenario scenario = node.scenario();
        final boolean scenarioFailed = (null != scenario)
                && (scenario.hasFailed() || scenario.getExecutionStatus() == ExecutionStatus.FAILED);

        final _Result res = node.result();
        if (null == res) {
            return scenarioFailed ? new Classification(true, false, false, 0) : Classification.EMPTY;
        }

        final List<_LogRecord> events = res.getEvents();
        final int eventCount = (null != events) ? events.size() : 0;
        if (0 == eventCount) {
            final boolean failure = scenarioFailed || res.status() == ExecutionStatus.FAILED;
            final boolean warning = !failure && res.status() == ExecutionStatus.WARNING;
            return new Classification(failure, warning, false, 0);
        }

        final ExpectedResponsesStore expStore = TreeHtml.getExpectedResponsesStore();
        final int responsesSize = (null != expStore && null != expStore.getResponses()) ? expStore.getResponses().size() : 0;
        final List<KnownIssue> knownIssues = (null != node.knownIssues()) ? node.knownIssues() : Collections.emptyList();
        final String nodeKey = buildNodeKey(node);
        final _LogRecord firstRecord = events.getFirst();

        final ResultCursor cursor;
        synchronized (CURSOR_LOCK) {
            final ResultCursor existing = CURSORS.get(res);
            if (isValid(existing, expStore, responsesSize, nodeKey, knownIssues, events, eventCount, firstRecord)) {
                cursor = existing;
            } else {
                final List<ExpectedResponse> matching = filterMatchingResponses(expStore, node);
                cursor = new ResultCursor(
                        expStore,
                        responsesSize,
                        nodeKey,
                        knownIssues.isEmpty() ? Collections.emptyList() : new ArrayList<>(knownIssues),
                        matching,
                        false);
                CURSORS.put(res, cursor);
            }

            if (cursor.scannedUpTo < eventCount) {
                advance(cursor, events, eventCount, firstRecord);
            }

            final boolean matchedExpected = cursor.nodeMatchedExpected;
            final boolean hasFailure = scenarioFailed
                    || cursor.nonBrowserFailure
                    || (!matchedExpected && cursor.browserFailure);
            final boolean hasWarning = !hasFailure
                    && (cursor.nonBrowserWarning || (!matchedExpected && cursor.browserWarning));
            final boolean hasNotice = !hasFailure && !hasWarning
                    && (cursor.nonBrowserNotice || (!matchedExpected && cursor.browserNotice));
            final int unclassified = cursor.nonBrowserCount + (matchedExpected ? 0 : cursor.browserWithDataCount);
            return new Classification(hasFailure, hasWarning, hasNotice, unclassified);
        }
    }

    private static boolean isValid(
            final ResultCursor cursor,
            final ExpectedResponsesStore expStore,
            final int responsesSize,
            final String nodeKey,
            final List<KnownIssue> knownIssues,
            final List<_LogRecord> events,
            final int eventCount,
            final _LogRecord firstRecord) {
        if (null == cursor) return false;
        if (cursor.expStore != expStore || cursor.responsesSize != responsesSize) return false;
        if (!cursor.nodeKey.equals(nodeKey)) return false;
        if (!cursor.knownIssuesSnapshot.equals(knownIssues)) return false;
        if (cursor.scannedUpTo > eventCount) return false;
        if (cursor.scannedUpTo > 0) {
            return cursor.firstRecord == firstRecord && events.get(cursor.scannedUpTo - 1) == cursor.lastRecord;
        }
        return true;
    }

    private static void advance(
            final ResultCursor cursor,
            final List<_LogRecord> events,
            final int eventCount,
            final _LogRecord firstRecord) {
        final boolean hasExpStore = null != cursor.expStore;
        final List<ExpectedResponse> matchingResponses = cursor.matchingResponses;
        final List<KnownIssue> knownIssues = cursor.knownIssuesSnapshot;

        for (int i = cursor.scannedUpTo; i < eventCount; i++) {
            final _LogRecord lr = events.get(i);
            final boolean hasData = null != lr && null != lr.data();
            if (hasExpStore && hasData && matchesAnyResponse(matchingResponses, lr)) {
                cursor.nodeMatchedExpected = true;
                continue;
            }

            final String text = hasData ? lr.text() : null;
            final boolean isBrowserText = hasExpStore && null != text && text.contains("[BROWSER]");

            // Unclassified count: original TreeHtml.getUnclassifiedEventCount checks (null != lr.data()) before [BROWSER]
            if (isBrowserText && hasData) {
                cursor.browserWithDataCount++;
            } else {
                cursor.nonBrowserCount++;
            }

            if (!hasData) {
                cursor.nonBrowserWarning = true;
                continue;
            }

            if (lr.data() instanceof _Event ev) {
                final Severity sev = ev.severity();
                if (Severity.S0 == sev || Severity.S1 == sev) {
                    if (isBrowserText) {
                        cursor.browserWarning = true;
                    } else {
                        cursor.nonBrowserWarning = true;
                    }
                    if (!isDecreasedToWarning(knownIssues, lr)) {
                        if (isBrowserText) {
                            cursor.browserFailure = true;
                        } else {
                            cursor.nonBrowserFailure = true;
                        }
                    }
                } else if (Severity.S2 == sev) {
                    if (isBrowserText) {
                        cursor.browserWarning = true;
                    } else {
                        cursor.nonBrowserWarning = true;
                    }
                } else if (Severity.S3 == sev) {
                    if (isBrowserText) {
                        cursor.browserNotice = true;
                    } else {
                        cursor.nonBrowserNotice = true;
                    }
                }
            }
        }

        cursor.scannedUpTo = eventCount;
        cursor.firstRecord = firstRecord;
        cursor.lastRecord = events.get(eventCount - 1);
    }

    private static List<ExpectedResponse> filterMatchingResponses(
            final ExpectedResponsesStore expStore,
            final NodeData node) {
        if (null == expStore || null == expStore.getResponses() || expStore.getResponses().isEmpty()) {
            return Collections.emptyList();
        }
        final List<ExpectedResponse> matched = new ArrayList<>(4);
        for (final ExpectedResponse er : expStore.getResponses()) {
            if (!KnownIssuesMatcher.isCatchAllRule(er.scenario(), er.pattern())
                    && ExpectedResponsesMatcher.matchesNodeScenario(er.scenario(), node)) {
                matched.add(er);
            }
        }
        return matched.isEmpty() ? Collections.emptyList() : matched;
    }

    private static boolean matchesAnyResponse(final List<ExpectedResponse> matchingResponses, final _LogRecord lr) {
        for (int i = 0; i < matchingResponses.size(); i++) {
            if (ExpectedResponsesMatcher.matchesPatternForRecord(matchingResponses.get(i).pattern(), lr)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isDecreasedToWarning(final List<KnownIssue> knownIssues, final _LogRecord lr) {
        for (int i = 0; i < knownIssues.size(); i++) {
            final KnownIssue ki = knownIssues.get(i);
            if (ki.decreaseToWarning()
                    && !KnownIssuesMatcher.isCatchAllRule(ki.scenario(), ki.pattern())
                    && KnownIssuesMatcher.matchesPatternForRecord(ki.pattern(), lr)) {
                return true;
            }
        }
        return false;
    }

    private static String buildNodeKey(final NodeData node) {
        final String scen = (null != node.scenario())
                ? node.scenario().getName() + '\0' + node.scenario().getShortName()
                : "";
        return scen + '\0' + (null != node.label() ? node.label() : "") + '\0' + (null != node.group() ? node.group() : "");
    }
}
