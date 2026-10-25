package works.lysenko.base.output;

import works.lysenko.base.issues.KnownIssue;
import works.lysenko.base.issues.KnownIssuesMatcher;
import works.lysenko.base.issues.KnownIssuesStore;
import works.lysenko.util.apis.log._LogRecord;
import works.lysenko.util.spec.Level;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
            final Map<works.lysenko.util.apis.scenario._Scenario, works.lysenko.util.apis.data._Result> sorted = core.getResults().getSorted();

            int newIssuesCount = 0;
            for (final _LogRecord lr : exec.issues().latestCopy()) {
                if (store.getForLogRecord(sorted.keySet(), lr).isEmpty()) {
                    newIssuesCount++;
                }
            }

            section(b(s1(newIssuesCount, c(EVENT)), SUMMARY));
            log(Level.none, s((isDebug() ? ansi(e(b(c(NOTICES), AND), true), CYAN) : EMPTY), ansi(b(c(NEW), s(c(ISSUES),
                    _COLON_)), RED)), true);
            for (final _LogRecord lr : exec.issues().latestCopy()) {
                if (store.getForLogRecord(sorted.keySet(), lr).isEmpty()) {
                    final int totalTests = (isNotNull(core) && isNotNull(core.getTest())) ? core.getTotalTests() : 0;
                    log(Level.none, ansi(lr.render(totalTests, -1, null)), false);
                }
            }
        }
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

        final Set<String> confirmedRefs = new LinkedHashSet<>();
        final Set<String> notReproducedRefs = new LinkedHashSet<>();
        final Set<String> notCheckedRefs = new LinkedHashSet<>();

        for (final KnownIssue ki : allKnownIssues) {
            boolean isConfirmed = false;
            for (final _LogRecord lr : logRecords) {
                if (store.getForLogRecord(sorted.keySet(), lr).contains(ki)) {
                    isConfirmed = true;
                    break;
                }
            }
            if (!isConfirmed) {
                for (final Map.Entry<works.lysenko.util.apis.scenario._Scenario, works.lysenko.util.apis.data._Result> entry : sorted.entrySet()) {
                    if (store.getForNode(entry.getKey(), entry.getValue()).contains(ki)) {
                        isConfirmed = true;
                        break;
                    }
                    if (null != entry.getValue() && null != entry.getValue().getEvents()) {
                        for (final _LogRecord lr : entry.getValue().getEvents()) {
                            if (store.getForLogRecord(List.of(entry.getKey()), lr).contains(ki)) {
                                isConfirmed = true;
                                break;
                            }
                        }
                    }
                    if (isConfirmed) break;
                }
            }

            final String ref = getReference(ki);

            if (isConfirmed) {
                confirmedRefs.add(ref);
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
            for (final String ref : confirmedRefs) {
                log(Level.none, s("  ", _BULLT_, _SPACE_, ref), false);
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

