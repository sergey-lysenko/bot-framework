package works.lysenko.base.output;

import works.lysenko.base.issues.KnownIssuesStore;
import works.lysenko.util.apis.log._LogRecord;
import works.lysenko.util.spec.Level;

import java.util.List;
import java.util.Map;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static works.lysenko.Base.*;
import static works.lysenko.util.chrs.__.NO;
import static works.lysenko.util.chrs.___.AND;
import static works.lysenko.util.chrs.___.NEW;
import static works.lysenko.util.data.enums.Ansi.*;
import static works.lysenko.util.data.strs.Bind.b;
import static works.lysenko.util.data.strs.Case.c;
import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.data.strs.Swap.s1;
import static works.lysenko.util.data.strs.Wrap.e;
import static works.lysenko.util.lang.word.E.EVENT;
import static works.lysenko.util.lang.word.I.ISSUES;
import static works.lysenko.util.lang.word.N.NOTICES;
import static works.lysenko.util.lang.word.S.SUMMARY;
import static works.lysenko.util.spec.Symbols._COLON_;

@SuppressWarnings({"FeatureEnvy", "NestedMethodCall", "ForeachStatement", "AutoBoxing"})
record NewIssues() {

    /**
     * Prints new issues to the console.
     */
    private static void consoleNewIssues() {

        if (exec.issues().isLatestEmpty())
            log(Level.none, ansi(b(c(NO), NEW, c(ISSUES)), GREEN), true);
        else {
            final KnownIssuesStore store = new KnownIssuesStore();
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
                    log(Level.none, ansi(lr.render(core.getTotalTests(), -1, null)), false);
                }
            }
        }
    }

    /**
     * This method performs the necessary actions for handling new issues.
     * It first retrieves a copy of the new issues from the execution context.
     * Then, it prints the new issues to the console and writes them to files.
     */
    static void run() {

        final List<_LogRecord> newIssues = exec.issues().latestCopy();
        consoleNewIssues();
    }
}
