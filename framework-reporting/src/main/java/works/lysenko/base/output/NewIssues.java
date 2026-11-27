package works.lysenko.base.output;

import works.lysenko.base.issues.KnownIssue;
import works.lysenko.base.issues.KnownIssuesMatcher;
import works.lysenko.base.issues.KnownIssuesStore;
import works.lysenko.base.logger.renderer.AnsiLogRenderer;
import works.lysenko.base.logger.renderer.LogRenderer;
import works.lysenko.base.responses.ExpectedResponsesStore;
import works.lysenko.util.apis.data._Event;
import works.lysenko.util.apis.data._Result;
import works.lysenko.util.apis.log._LogRecord;
import works.lysenko.util.apis.scenario._Scenario;
import works.lysenko.util.data.enums.Severity;
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
    private static KnownIssuesStore fixedIssuesStore = null;

    public static void setKnownIssuesStore(final KnownIssuesStore store) {
        knownIssuesStore = store;
    }

    private static KnownIssuesStore getKnownIssuesStore() {
        return (null != knownIssuesStore) ? knownIssuesStore : KnownIssuesStore.getGlobalStore();
    }

    public static void setFixedIssuesStore(final KnownIssuesStore store) {
        fixedIssuesStore = store;
    }

    private static KnownIssuesStore getFixedIssuesStore() {
        return (null != fixedIssuesStore) ? fixedIssuesStore : KnownIssuesStore.getFixedGlobalStore();
    }

    private static boolean isFailureSeverity(final _LogRecord lr) {
        if (null == lr) return false;
        if (lr.data() instanceof _Event ev) {
            final Severity sev = ev.severity();
            return Severity.S0 == sev || Severity.S1 == sev;
        }
        return false;
    }

    /**
     * Prints regressions and new issues to the console.
     */
    private static void consoleNewIssues() {

        if (exec.issues().isLatestEmpty())
            log(Level.none, ansi(b(c(NO), NEW, c(ISSUES)), GREEN), true);
        else {
            final KnownIssuesStore store = getKnownIssuesStore();
            final KnownIssuesStore fixedStore = getFixedIssuesStore();
            final ExpectedResponsesStore expStore = TreeHtml.getExpectedResponsesStore();
            final Map<_Scenario, _Result> sorted = core.getResults().getSorted();
            final Map<_LogRecord, _Scenario> recordOwners = indexRecordOwners(sorted);

            final List<MatchedRegression> failureRegressions = new ArrayList<>();
            final List<MatchedRegression> warningRegressions = new ArrayList<>();
            final List<_LogRecord> unmatchedRecords = new ArrayList<>();
            for (final _LogRecord lr : exec.issues().latestCopy()) {
                if (isExpectedRecord(expStore, sorted, recordOwners, lr)) {
                    continue;
                }
                final List<KnownIssue> matched = matchKnownIssuesForRecord(store, recordOwners, lr);
                if (!matched.isEmpty()) {
                    continue;
                }
                final List<KnownIssue> matchedFixed = matchKnownIssuesForRecord(fixedStore, recordOwners, lr);
                if (!matchedFixed.isEmpty()) {
                    if (isFailureSeverity(lr)) {
                        failureRegressions.add(new MatchedRegression(lr, matchedFixed));
                    } else {
                        warningRegressions.add(new MatchedRegression(lr, matchedFixed));
                    }
                } else {
                    unmatchedRecords.add(lr);
                }
            }

            final LogRenderer ansiRenderer = new AnsiLogRenderer();
            final int totalTests = (isNotNull(core) && isNotNull(core.getTest()) && isNotNull(core.getTotalTests())) ? core.getTotalTests() : 0;
            final int spanLength = (isNotNull(core) && isNotNull(core.getLogger())) ? core.getLogger().getSpanLength() : 0;

            if (!failureRegressions.isEmpty()) {
                section(b(s1(failureRegressions.size(), "Regression"), SUMMARY));
                log(Level.none, ansi("REGRESSIONS:", MAGENTA_BOLD_BRIGHT), true);
                for (final MatchedRegression mr : failureRegressions) {
                    log(Level.none, ansi(formatRegressionHeader(mr.issues()), MAGENTA_BOLD_BRIGHT), false);
                    log(Level.none, ansi(ansiRenderer.render(mr.record(), totalTests, spanLength, null)), false);
                }
            }

            if (!warningRegressions.isEmpty()) {
                section(b(s1(warningRegressions.size(), "Regression Warning"), SUMMARY));
                log(Level.none, ansi("REGRESSION WARNINGS:", MAGENTA), true);
                for (final MatchedRegression mr : warningRegressions) {
                    log(Level.none, ansi(formatRegressionHeader(mr.issues()), MAGENTA), false);
                    log(Level.none, ansi(ansiRenderer.render(mr.record(), totalTests, spanLength, null)), false);
                }
            }

            section(b(s1(unmatchedRecords.size(), c(EVENT)), SUMMARY));
            log(Level.none, s((isDebug() ? ansi(e(b(c(NOTICES), AND), true), CYAN) : EMPTY), ansi(b(c(NEW), s(c(ISSUES),
                    _COLON_)), RED)), true);
            for (final _LogRecord lr : unmatchedRecords) {
                log(Level.none, ansi(ansiRenderer.render(lr, totalTests, spanLength, null)), false);
            }
        }
    }

    private record MatchedRegression(_LogRecord record, List<KnownIssue> issues) {}

    private static String formatRegressionHeader(final List<KnownIssue> issues) {
        final StringBuilder sb = new StringBuilder("  ↩ Matches fixed issue: ");
        for (int i = 0; i < issues.size(); i++) {
            if (i > 0) sb.append(", ");
            final KnownIssue fi = issues.get(i);
            sb.append(getReference(fi));
            if (null != fi.title() && !fi.title().isBlank() && !fi.title().trim().equals(getReference(fi))) {
                sb.append(" — ").append(fi.title().trim());
            }
            if (null != fi.resolvedAt() && !fi.resolvedAt().isBlank()) {
                sb.append(" [resolved ").append(fi.resolvedAt().trim()).append(']');
            }
        }
        return sb.toString();
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
     * Computes which known issues were confirmed (with reproducing tests), not reproduced or not checked,
     * and evaluates fixed issues from {@code fixed.issues.json} for regressions.
     *
     * @return summary of known and fixed issues for this run
     */
    static KnownIssuesSummary computeKnownIssuesSummary() {

        final KnownIssuesStore store = getKnownIssuesStore();
        final KnownIssuesStore fixedStore = getFixedIssuesStore();
        final ExpectedResponsesStore expStore = TreeHtml.getExpectedResponsesStore();
        final List<KnownIssue> allKnownIssues = (null != store) ? store.getIssues() : Collections.emptyList();
        final List<KnownIssue> allFixedIssues = (null != fixedStore) ? fixedStore.getIssues() : Collections.emptyList();
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
        final Set<_LogRecord> activeKnownMatchedRecords = Collections.newSetFromMap(new IdentityHashMap<>());

        final Map<KnownIssue, Set<Integer>> fixedFailureTests = new HashMap<>();
        final Set<KnownIssue> regressedFixedIssues = new HashSet<>();
        final Map<KnownIssue, Set<Integer>> fixedWarningTests = new HashMap<>();
        final Set<KnownIssue> regressedWarningFixedIssues = new HashSet<>();

        for (final _LogRecord lr : allRecords) {
            if (isExpectedRecord(expStore, sorted, recordOwners, lr)) {
                continue;
            }
            final List<KnownIssue> matched = matchKnownIssuesForRecord(store, recordOwners, lr);
            if (!matched.isEmpty()) {
                activeKnownMatchedRecords.add(lr);
                for (final KnownIssue ki : matched) {
                    confirmedIssues.add(ki);
                    if (lr.test() > 0) {
                        issueTests.computeIfAbsent(ki, _ -> new TreeSet<>()).add(lr.test());
                    }
                }
            }
        }

        if (!allFixedIssues.isEmpty()) {
            for (final _LogRecord lr : allRecords) {
                if (activeKnownMatchedRecords.contains(lr) || isExpectedRecord(expStore, sorted, recordOwners, lr)) {
                    continue;
                }
                final List<KnownIssue> matchedFixed = matchKnownIssuesForRecord(fixedStore, recordOwners, lr);
                if (matchedFixed.isEmpty()) {
                    continue;
                }
                final boolean failTier = isFailureSeverity(lr);
                for (final KnownIssue fi : matchedFixed) {
                    if (failTier) {
                        regressedFixedIssues.add(fi);
                        if (lr.test() > 0) {
                            fixedFailureTests.computeIfAbsent(fi, _ -> new TreeSet<>()).add(lr.test());
                        }
                    } else {
                        regressedWarningFixedIssues.add(fi);
                        if (lr.test() > 0) {
                            fixedWarningTests.computeIfAbsent(fi, _ -> new TreeSet<>()).add(lr.test());
                        }
                    }
                }
            }
        }

        // Aggregate and deduplicate active issues by reference with precedence: Confirmed > NOT Reproduced > NOT Checked
        final Map<String, RefAggregation> aggregated = new LinkedHashMap<>();
        for (final KnownIssue ki : allKnownIssues) {
            final String ref = getReference(ki);
            final RefAggregation agg = aggregated.computeIfAbsent(ref, _ -> new RefAggregation(ki, ref));
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

        // Aggregate fixed issues by reference with precedence:
        // Regressed (S0/S1) > Regressed Warning (S2/S3/untyped) > Verified Fixed > Fixed NOT Checked
        final Map<String, FixedRefAggregation> fixedAggregated = new LinkedHashMap<>();
        for (final KnownIssue fi : allFixedIssues) {
            final String ref = getReference(fi);
            final FixedRefAggregation agg = fixedAggregated.computeIfAbsent(ref, _ -> new FixedRefAggregation(fi, ref));
            if (regressedFixedIssues.contains(fi)) {
                if (!agg.regressed) {
                    agg.representative = fi;
                }
                agg.regressed = true;
                agg.tests.addAll(fixedFailureTests.getOrDefault(fi, Collections.emptySet()));
            }
            if (regressedWarningFixedIssues.contains(fi)) {
                if (!agg.regressed && !agg.regressedWarning) {
                    agg.representative = fi;
                }
                agg.regressedWarning = true;
                agg.tests.addAll(fixedWarningTests.getOrDefault(fi, Collections.emptySet()));
            }
            if (isTargetExecuted(fi.scenario(), sorted, executedPhases)) {
                agg.executed = true;
            }
        }

        final Map<String, KnownIssuesSummary.Entry> regressedRefs = new LinkedHashMap<>();
        final Map<String, KnownIssuesSummary.Entry> regressedWarningRefs = new LinkedHashMap<>();
        final Map<String, KnownIssuesSummary.Entry> verifiedFixedRefs = new LinkedHashMap<>();
        final Map<String, KnownIssuesSummary.Entry> fixedNotCheckedRefs = new LinkedHashMap<>();

        for (final FixedRefAggregation agg : fixedAggregated.values()) {
            if (agg.regressed) {
                regressedRefs.put(agg.ref, entryOf(agg.representative, agg.ref, agg.tests));
            } else if (agg.regressedWarning) {
                regressedWarningRefs.put(agg.ref, entryOf(agg.representative, agg.ref, agg.tests));
            } else if (agg.executed) {
                verifiedFixedRefs.put(agg.ref, entryOf(agg.representative, agg.ref, Collections.emptySet()));
            } else {
                fixedNotCheckedRefs.put(agg.ref, entryOf(agg.representative, agg.ref, Collections.emptySet()));
            }
        }

        return new KnownIssuesSummary(
                confirmedRefs, notReproducedRefs, notCheckedRefs,
                regressedRefs, regressedWarningRefs, verifiedFixedRefs, fixedNotCheckedRefs);
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

    private static final class FixedRefAggregation {
        final String ref;
        KnownIssue representative;
        boolean regressed;
        boolean regressedWarning;
        boolean executed;
        final Set<Integer> tests = new TreeSet<>();

        FixedRefAggregation(final KnownIssue representative, final String ref) {
            this.representative = representative;
            this.ref = ref;
        }
    }

    /**
     * Prints summary information about known issues and fixed-issue regression verification to the console.
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
                logSummaryEntry(entry);
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

        if (summary.hasFixedIssues()) {
            section("Fixed Issues Verification");

            log(Level.none, ansi("REGRESSIONS (FIXED ISSUES REPRODUCED):", MAGENTA_BOLD_BRIGHT), true);
            if (summary.regressed().isEmpty()) {
                log(Level.none, s("  ", c(NONE)), false);
            } else {
                for (final KnownIssuesSummary.Entry entry : summary.regressed().values()) {
                    logSummaryEntry(entry);
                }
            }

            if (!summary.regressedWarnings().isEmpty()) {
                log(Level.none, ansi("REGRESSION WARNINGS:", MAGENTA), true);
                for (final KnownIssuesSummary.Entry entry : summary.regressedWarnings().values()) {
                    logSummaryEntry(entry);
                }
            }

            log(Level.none, ansi("VERIFIED FIXED:", GREEN), true);
            if (summary.verifiedFixed().isEmpty()) {
                log(Level.none, s("  ", c(NONE)), false);
            } else {
                for (final String ref : summary.verifiedFixed().keySet()) {
                    log(Level.none, s("  ", _BULLT_, _SPACE_, ref), false);
                }
            }

            log(Level.none, ansi("FIXED ISSUES NOT CHECKED:", CYAN), true);
            if (summary.fixedNotChecked().isEmpty()) {
                log(Level.none, s("  ", c(NONE)), false);
            } else {
                for (final String ref : summary.fixedNotChecked().keySet()) {
                    log(Level.none, s("  ", _BULLT_, _SPACE_, ref), false);
                }
            }
        }
    }

    private static void logSummaryEntry(final KnownIssuesSummary.Entry entry) {
        final Set<Integer> testNums = entry.tests();
        if (testNums.isEmpty()) {
            log(Level.none, s("  ", _BULLT_, _SPACE_, entry.ref()), false);
        } else {
            final String testsStr = testNums.stream().map(Object::toString).collect(Collectors.joining(" "));
            log(Level.none, s("  ", _BULLT_, _SPACE_, entry.ref(), _SPACE_, testsStr), false);
        }
    }

    private static KnownIssuesSummary.Entry entryOf(final KnownIssue ki, final String ref, final Set<Integer> tests) {
        return new KnownIssuesSummary.Entry(
                ref,
                ki.link().trim(),
                ki.title().trim(),
                ki.description().trim(),
                tests,
                ki.resolvedAt().trim(),
                ki.verifiedInRun().trim());
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

