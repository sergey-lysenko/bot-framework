package works.lysenko.base.output;

import works.lysenko.base.issues.KnownIssue;
import works.lysenko.base.issues.KnownIssuesMatcher;
import works.lysenko.base.logger.renderer.AnsiLogRenderer;
import works.lysenko.base.logger.renderer.LogRenderer;
import works.lysenko.base.issues.KnownIssuesStore;
import works.lysenko.util.apis.log._LogRecord;
import works.lysenko.util.spec.Level;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static works.lysenko.Base.*;
import static works.lysenko.util.chrs.__.NO;
import static works.lysenko.util.chrs.___.AND;
import static works.lysenko.util.chrs.___.NEW;
import static works.lysenko.util.chrs.___.NOT;
import static works.lysenko.util.data.enums.Ansi.*;
import static works.lysenko.util.data.strs.Bind.b;
import static works.lysenko.util.data.strs.Case.c;
import static works.lysenko.util.data.strs.Case.u;
import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.data.strs.Swap.s1;
import static works.lysenko.util.data.strs.Wrap.e;
import static works.lysenko.util.func.type.Objects.isNotNull;
import static works.lysenko.util.lang.word.C.CHECKED;
import static works.lysenko.util.lang.word.C.CONFIRMED;
import static works.lysenko.util.lang.word.E.EVENT;
import static works.lysenko.util.lang.word.I.ISSUES;
import static works.lysenko.util.lang.word.K.KNOWN;
import static works.lysenko.util.lang.word.N.NONE;
import static works.lysenko.util.lang.word.N.NOTICES;
import static works.lysenko.util.lang.word.R.REPRODUCED;
import static works.lysenko.util.lang.word.S.SUMMARY;
import static works.lysenko.util.spec.Symbols._BULLT_;
import static works.lysenko.util.spec.Symbols._COLON_;
import static works.lysenko.util.spec.Symbols._SPACE_;

@SuppressWarnings({"FeatureEnvy", "NestedMethodCall", "ForeachStatement", "AutoBoxing"})
record NewIssues() {

    private static KnownIssuesStore knownIssuesStore = null;

    public static void setKnownIssuesStore(final KnownIssuesStore store) {
        knownIssuesStore = store;
    }

    private static KnownIssuesStore getKnownIssuesStore() {
        if (null == knownIssuesStore) {
            knownIssuesStore = new KnownIssuesStore();
        }
        return knownIssuesStore;
    }

    /**
     * Prints new issues to the console.
     */
    private static void consoleNewIssues() {

        if (exec.issues().isLatestEmpty())
            log(Level.none, ansi(b(c(NO), NEW, c(ISSUES)), GREEN), true);
        else {
            final KnownIssuesStore store = getKnownIssuesStore();
            final works.lysenko.base.responses.ExpectedResponsesStore expStore = TreeHtml.getExpectedResponsesStore();
            final Map<works.lysenko.util.apis.scenario._Scenario, works.lysenko.util.apis.data._Result> sorted = core.getResults().getSorted();

            final List<_LogRecord> unmatchedRecords = new java.util.ArrayList<>();
            for (final _LogRecord lr : exec.issues().latestCopy()) {
                if (store.getForLogRecord(sorted.keySet(), lr).isEmpty() && !isExpectedRecord(expStore, sorted, lr)) {
                    unmatchedRecords.add(lr);
                }
            }

            section(b(s1(unmatchedRecords.size(), c(EVENT)), SUMMARY));
            log(Level.none, s((isDebug() ? ansi(e(b(c(NOTICES), AND), true), CYAN) : EMPTY), ansi(b(c(NEW), s(c(ISSUES),
                    _COLON_)), RED)), true);
            final LogRenderer ansiRenderer = new AnsiLogRenderer();
            final int totalTests = (isNotNull(core) && isNotNull(core.getTest()) && isNotNull(core.getTotalTests())) ? core.getTotalTests() : 0;
            final int spanLength = (isNotNull(core) && isNotNull(core.getLogger())) ? core.getLogger().getSpanLength() : 0;
            for (final _LogRecord lr : unmatchedRecords) {
                log(Level.none, ansi(ansiRenderer.render(lr, totalTests, spanLength, null)), false);
            }
        }
    }

    private static boolean isExpectedRecord(
            final works.lysenko.base.responses.ExpectedResponsesStore expStore,
            final Map<works.lysenko.util.apis.scenario._Scenario, works.lysenko.util.apis.data._Result> sorted,
            final _LogRecord lr) {
        if (null == expStore || null == lr) return false;
        if (null != sorted) {
            for (final Map.Entry<works.lysenko.util.apis.scenario._Scenario, works.lysenko.util.apis.data._Result> entry : sorted.entrySet()) {
                final works.lysenko.util.apis.data._Result res = entry.getValue();
                if (null != res && null != res.getEvents() && res.getEvents().contains(lr)) {
                    if (expStore.isExpected(entry.getKey(), lr) || expStore.matchesNode(entry.getKey(), res)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Prints summary information about known issues to the console.
     */
    private static void consoleKnownIssues() {

        final KnownIssuesStore store = getKnownIssuesStore();
        final List<KnownIssue> allKnownIssues = store.getIssues();
        final Map<works.lysenko.util.apis.scenario._Scenario, works.lysenko.util.apis.data._Result> sorted =
                (isNotNull(core) && isNotNull(core.getResults())) ? core.getResults().getSorted() : Collections.emptyMap();
        final List<_LogRecord> logRecords =
                (isNotNull(exec) && isNotNull(exec.issues())) ? exec.issues().latestCopy() : Collections.emptyList();

        final Map<String, Set<Integer>> confirmedRefs = new LinkedHashMap<>();
        final Set<String> notReproducedRefs = new LinkedHashSet<>();
        final Set<String> notCheckedRefs = new LinkedHashSet<>();

        final Map<KnownIssue, Set<Integer>> issueTests = new java.util.HashMap<>();
        final Set<KnownIssue> confirmedIssues = new java.util.HashSet<>();

        for (final _LogRecord lr : logRecords) {
            final List<KnownIssue> matched = store.getForLogRecord(sorted.keySet(), lr);
            for (final KnownIssue ki : matched) {
                confirmedIssues.add(ki);
                if (lr.test() > 0) {
                    issueTests.computeIfAbsent(ki, k -> new TreeSet<>()).add(lr.test());
                }
            }
        }
        for (final Map.Entry<works.lysenko.util.apis.scenario._Scenario, works.lysenko.util.apis.data._Result> entry : sorted.entrySet()) {
            final works.lysenko.util.apis.scenario._Scenario scenario = entry.getKey();
            final works.lysenko.util.apis.data._Result result = entry.getValue();
            final List<KnownIssue> nodeMatches = store.getForNode(scenario, result);
            confirmedIssues.addAll(nodeMatches);
            if (null != result && null != result.getEvents()) {
                final List<works.lysenko.util.apis.scenario._Scenario> singleScenario = List.of(scenario);
                for (final _LogRecord lr : result.getEvents()) {
                    if (lr.test() > 0) {
                        for (final KnownIssue ki : nodeMatches) {
                            issueTests.computeIfAbsent(ki, k -> new TreeSet<>()).add(lr.test());
                        }
                    }
                    final List<KnownIssue> eventMatches = store.getForLogRecord(singleScenario, lr);
                    for (final KnownIssue ki : eventMatches) {
                        confirmedIssues.add(ki);
                        if (lr.test() > 0) {
                            issueTests.computeIfAbsent(ki, k -> new TreeSet<>()).add(lr.test());
                        }
                    }
                }
            }
        }

        for (final KnownIssue ki : allKnownIssues) {
            final Set<Integer> reproducingTests = issueTests.getOrDefault(ki, Collections.emptySet());
            final boolean isConfirmed = confirmedIssues.contains(ki);

            final String ref = getReference(ki);

            if (isConfirmed) {
                confirmedRefs.computeIfAbsent(ref, k -> new TreeSet<>()).addAll(reproducingTests);
            } else {
                final String target = ki.scenario();
                final boolean scenarioExecuted;
                if (null == target || target.isBlank() || "*".equals(target.trim())) {
                    scenarioExecuted = !sorted.isEmpty();
                } else {
                    scenarioExecuted = KnownIssuesMatcher.matchesAnyScenario(target, sorted.keySet());
                }

                if (scenarioExecuted) {
                    notReproducedRefs.add(ref);
                } else {
                    notCheckedRefs.add(ref);
                }
            }
        }

        section(b(c(KNOWN), c(ISSUES), SUMMARY));

        log(Level.none, ansi(b(c(CONFIRMED), s(c(KNOWN), _SPACE_, c(ISSUES), _COLON_)), GREEN), true);
        if (confirmedRefs.isEmpty()) {
            log(Level.none, s("  ", c(NONE)), false);
        } else {
            for (final Map.Entry<String, Set<Integer>> entry : confirmedRefs.entrySet()) {
                final String ref = entry.getKey();
                final Set<Integer> testNums = entry.getValue();
                if (testNums.isEmpty()) {
                    log(Level.none, s("  ", _BULLT_, _SPACE_, ref), false);
                } else {
                    final String testsStr = testNums.stream().map(Object::toString).collect(Collectors.joining(" "));
                    log(Level.none, s("  ", _BULLT_, _SPACE_, ref, _SPACE_, testsStr), false);
                }
            }
        }

        log(Level.none, ansi(b(c(KNOWN), c(ISSUES), u(NOT), s(c(REPRODUCED), _COLON_)), CYAN), true);
        if (notReproducedRefs.isEmpty()) {
            log(Level.none, s("  ", c(NONE)), false);
        } else {
            for (final String ref : notReproducedRefs) {
                log(Level.none, s("  ", _BULLT_, _SPACE_, ref), false);
            }
        }

        log(Level.none, ansi(b(c(KNOWN), c(ISSUES), u(NOT), s(c(CHECKED), _COLON_)), CYAN), true);
        if (notCheckedRefs.isEmpty()) {
            log(Level.none, s("  ", c(NONE)), false);
        } else {
            for (final String ref : notCheckedRefs) {
                log(Level.none, s("  ", _BULLT_, _SPACE_, ref), false);
            }
        }
    }

    private static String getReference(final KnownIssue ki) {

        if (null != ki.link() && !ki.link().isBlank()) {
            return ki.link().trim();
        }
        if (null != ki.title() && !ki.title().isBlank()) {
            return ki.title().trim();
        }
        if (null != ki.scenario() && !ki.scenario().isBlank()) {
            return ki.scenario().trim();
        }
        if (null != ki.pattern() && !ki.pattern().isBlank()) {
            return ki.pattern().trim();
        }
        return "Unknown issue";
    }

    /**
     * This method performs the necessary actions for handling new issues.
     * It first retrieves a copy of the new issues from the execution context.
     * Then, it prints the new issues to the console and writes them to files.
     */
    static void run() {

        final List<_LogRecord> newIssues = exec.issues().latestCopy();
        consoleNewIssues();
        consoleKnownIssues();
    }
}

