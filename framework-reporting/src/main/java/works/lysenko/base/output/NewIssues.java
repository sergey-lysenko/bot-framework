package works.lysenko.base.output;

import works.lysenko.base.issues.KnownIssue;
import works.lysenko.base.issues.KnownIssuesMatcher;
import works.lysenko.base.issues.KnownIssuesStore;
import works.lysenko.base.logger.renderer.AnsiLogRenderer;
import works.lysenko.base.logger.renderer.LogRenderer;
import works.lysenko.base.responses.ExpectedResponsesStore;
import works.lysenko.util.apis.data._Result;
import works.lysenko.util.apis.log._LogRecord;
import works.lysenko.util.apis.scenario._Scenario;
import works.lysenko.util.spec.Level;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
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
        return (null != knownIssuesStore) ? knownIssuesStore : KnownIssuesStore.getGlobalStore();
    }

    /**
     * Prints new issues to the console.
     */
    private static void consoleNewIssues() {

        if (exec.issues().isLatestEmpty())
            log(Level.none, ansi(b(c(NO), NEW, c(ISSUES)), GREEN), true);
        else {
            final KnownIssuesStore store = getKnownIssuesStore();
            final ExpectedResponsesStore expStore = TreeHtml.getExpectedResponsesStore();
            final Map<_Scenario, _Result> sorted = core.getResults().getSorted();
            final Map<_LogRecord, _Scenario> recordOwners = indexRecordOwners(sorted);

            final List<_LogRecord> unmatchedRecords = new ArrayList<>();
            for (final _LogRecord lr : exec.issues().latestCopy()) {
                if (isExpectedRecord(expStore, sorted, recordOwners, lr)) {
                    continue;
                }
                final List<KnownIssue> matched = matchKnownIssuesForRecord(store, recordOwners, lr);
                if (matched.isEmpty()) {
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

    private static Map<_LogRecord, _Scenario> indexRecordOwners(final Map<_Scenario, _Result> sorted) {
        final Map<_LogRecord, _Scenario> owners = new IdentityHashMap<>();
        if (null != sorted) {
            for (final Map.Entry<_Scenario, _Result> entry : sorted.entrySet()) {
                final _Scenario scenario = entry.getKey();
                final _Result res = entry.getValue();
                if (null != res && null != res.getEvents()) {
                    for (final _LogRecord lr : res.getEvents()) {
                        if (null != lr) {
                            owners.putIfAbsent(lr, scenario);
                        }
                    }
                }
            }
        }
        return owners;
    }

    private static _Scenario resolveOwner(final _LogRecord lr, final Map<_LogRecord, _Scenario> recordOwners) {
        if (null == lr) return null;
        return (null != lr.scenario()) ? lr.scenario() : recordOwners.get(lr);
    }

    private static List<KnownIssue> matchKnownIssuesForRecord(
            final KnownIssuesStore store,
            final Map<_LogRecord, _Scenario> recordOwners,
            final _LogRecord lr) {
        if (null == store || null == lr) return Collections.emptyList();
        final _Scenario owner = resolveOwner(lr, recordOwners);
        if (null != owner) {
            return store.getForLogRecord(owner, lr);
        }
        return store.getForContext(lr.context(), lr);
    }

    private static boolean isExpectedRecord(
            final ExpectedResponsesStore expStore,
            final Map<_Scenario, _Result> sorted,
            final Map<_LogRecord, _Scenario> recordOwners,
            final _LogRecord lr) {
        if (null == expStore || null == lr) return false;
        final _Scenario owner = resolveOwner(lr, recordOwners);
        if (null != owner) {
            if (expStore.isExpected(owner, lr)) {
                return true;
            }
            final _Result res = (null != sorted) ? sorted.get(owner) : null;
            final String text = lr.text();
            return null != text && text.contains("[BROWSER]") && null != res && expStore.matchesNode(owner, res);
        }
        return expStore.isExpected(lr.context(), lr);
    }

    /**
     * Computes which known issues were confirmed (with reproducing tests), not reproduced or not checked.
     *
     * @return summary of known issues for this run
     */
    static KnownIssuesSummary computeKnownIssuesSummary() {

        final KnownIssuesStore store = getKnownIssuesStore();
        final ExpectedResponsesStore expStore = TreeHtml.getExpectedResponsesStore();
        final List<KnownIssue> allKnownIssues = store.getIssues();
        final Map<_Scenario, _Result> sorted =
                (isNotNull(core) && isNotNull(core.getResults())) ? core.getResults().getSorted() : Collections.emptyMap();
        final Set<String> executedPhases =
                (isNotNull(core) && isNotNull(core.getResults())) ? core.getResults().getExecutedPhases() : Collections.emptySet();
        final List<_LogRecord> rootEvents =
                (isNotNull(core) && isNotNull(core.getResults())) ? core.getResults().getRootEvents() : Collections.emptyList();
        final List<_LogRecord> logRecords =
                (isNotNull(exec) && isNotNull(exec.issues())) ? exec.issues().latestCopy() : Collections.emptyList();

        final Map<_LogRecord, _Scenario> recordOwners = indexRecordOwners(sorted);

        // Collect all unique records across exec.issues(), scenario results, and root events (e.g. Pretest)
        final Set<_LogRecord> seenRecords = Collections.newSetFromMap(new IdentityHashMap<>());
        final List<_LogRecord> allRecords = new ArrayList<>(logRecords.size() + rootEvents.size());
        for (final _LogRecord lr : logRecords) {
            if (null != lr && seenRecords.add(lr)) {
                allRecords.add(lr);
            }
        }
        for (final _LogRecord lr : rootEvents) {
            if (null != lr && seenRecords.add(lr)) {
                allRecords.add(lr);
            }
        }
        for (final _Result res : sorted.values()) {
            if (null != res && null != res.getEvents()) {
                for (final _LogRecord lr : res.getEvents()) {
                    if (null != lr && seenRecords.add(lr)) {
                        allRecords.add(lr);
                    }
                }
            }
        }

        final Map<KnownIssue, Set<Integer>> issueTests = new HashMap<>();
        final Set<KnownIssue> confirmedIssues = new HashSet<>();

        for (final _LogRecord lr : allRecords) {
            if (isExpectedRecord(expStore, sorted, recordOwners, lr)) {
                continue;
            }
            final List<KnownIssue> matched = matchKnownIssuesForRecord(store, recordOwners, lr);
            for (final KnownIssue ki : matched) {
                confirmedIssues.add(ki);
                if (lr.test() > 0) {
                    issueTests.computeIfAbsent(ki, k -> new TreeSet<>()).add(lr.test());
                }
            }
        }

        // Aggregate and deduplicate by reference with precedence: Confirmed > NOT Reproduced > NOT Checked
        final Map<String, RefAggregation> aggregated = new LinkedHashMap<>();
        for (final KnownIssue ki : allKnownIssues) {
            final String ref = getReference(ki);
            final RefAggregation agg = aggregated.computeIfAbsent(ref, k -> new RefAggregation(ki, ref));
            if (confirmedIssues.contains(ki)) {
                if (!agg.confirmed) {
                    agg.representative = ki;
                }
                agg.confirmed = true;
                agg.tests.addAll(issueTests.getOrDefault(ki, Collections.emptySet()));
            }
            if (isTargetExecuted(ki.scenario(), sorted, executedPhases)) {
                agg.executed = true;
            }
        }

        final Map<String, KnownIssuesSummary.Entry> confirmedRefs = new LinkedHashMap<>();
        final Map<String, KnownIssuesSummary.Entry> notReproducedRefs = new LinkedHashMap<>();
        final Map<String, KnownIssuesSummary.Entry> notCheckedRefs = new LinkedHashMap<>();

        for (final RefAggregation agg : aggregated.values()) {
            if (agg.confirmed) {
                confirmedRefs.put(agg.ref, entryOf(agg.representative, agg.ref, agg.tests));
            } else if (agg.executed) {
                notReproducedRefs.put(agg.ref, entryOf(agg.representative, agg.ref, Collections.emptySet()));
            } else {
                notCheckedRefs.put(agg.ref, entryOf(agg.representative, agg.ref, Collections.emptySet()));
            }
        }

        return new KnownIssuesSummary(confirmedRefs, notReproducedRefs, notCheckedRefs);
    }

    private static boolean isTargetExecuted(
            final String target,
            final Map<_Scenario, _Result> sorted,
            final Set<String> executedPhases) {
        if (KnownIssuesMatcher.isWildcardScenario(target)) {
            return !sorted.isEmpty() || !executedPhases.isEmpty();
        }
        if (null == target || target.isBlank()) {
            return false;
        }
        if (KnownIssuesMatcher.matchesAnyScenario(target, sorted.keySet())) {
            return true;
        }
        for (final String phase : executedPhases) {
            if (null != phase && KnownIssuesMatcher.matchesScenario(target, phase, phase)) {
                return true;
            }
        }
        return false;
    }

    private static final class RefAggregation {
        final String ref;
        KnownIssue representative;
        boolean confirmed;
        boolean executed;
        final Set<Integer> tests = new TreeSet<>();

        RefAggregation(final KnownIssue representative, final String ref) {
            this.representative = representative;
            this.ref = ref;
        }
    }

    /**
     * Prints summary information about known issues to the console.
     *
     * @param summary precomputed summary
     */
    private static void consoleKnownIssues(final KnownIssuesSummary summary) {

        section(b(c(KNOWN), c(ISSUES), SUMMARY));

        log(Level.none, ansi(b(c(CONFIRMED), s(c(KNOWN), _SPACE_, c(ISSUES), _COLON_)), GREEN), true);
        if (summary.confirmed().isEmpty()) {
            log(Level.none, s("  ", c(NONE)), false);
        } else {
            for (final KnownIssuesSummary.Entry entry : summary.confirmed().values()) {
                final Set<Integer> testNums = entry.tests();
                if (testNums.isEmpty()) {
                    log(Level.none, s("  ", _BULLT_, _SPACE_, entry.ref()), false);
                } else {
                    final String testsStr = testNums.stream().map(Object::toString).collect(Collectors.joining(" "));
                    log(Level.none, s("  ", _BULLT_, _SPACE_, entry.ref(), _SPACE_, testsStr), false);
                }
            }
        }

        log(Level.none, ansi(b(c(KNOWN), c(ISSUES), u(NOT), s(c(REPRODUCED), _COLON_)), CYAN), true);
        if (summary.notReproduced().isEmpty()) {
            log(Level.none, s("  ", c(NONE)), false);
        } else {
            for (final String ref : summary.notReproduced().keySet()) {
                log(Level.none, s("  ", _BULLT_, _SPACE_, ref), false);
            }
        }

        log(Level.none, ansi(b(c(KNOWN), c(ISSUES), u(NOT), s(c(CHECKED), _COLON_)), CYAN), true);
        if (summary.notChecked().isEmpty()) {
            log(Level.none, s("  ", c(NONE)), false);
        } else {
            for (final String ref : summary.notChecked().keySet()) {
                log(Level.none, s("  ", _BULLT_, _SPACE_, ref), false);
            }
        }
    }

    private static KnownIssuesSummary.Entry entryOf(final KnownIssue ki, final String ref, final Set<Integer> tests) {
        return new KnownIssuesSummary.Entry(ref, ki.link().trim(), ki.title().trim(), ki.description().trim(), tests);
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

        consoleNewIssues();
        final KnownIssuesSummary summary = computeKnownIssuesSummary();
        consoleKnownIssues(summary);
        KnownIssuesHtml.write(summary);
    }
}

