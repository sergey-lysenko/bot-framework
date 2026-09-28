package works.lysenko.base.output;

import works.lysenko.util.apis.log._LogRecord;
import works.lysenko.util.spec.Level;

import java.util.List;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static works.lysenko.Base.*;
import static works.lysenko.base.output.File.fileNewIssues;
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
            section(b(s1(exec.issues().latest().size(), c(EVENT)), SUMMARY));
            log(Level.none, s((isDebug() ? ansi(e(b(c(NOTICES), AND), true), CYAN) : EMPTY), ansi(b(c(NEW), s(c(ISSUES),
                    _COLON_)), RED)), true);
            for (final _LogRecord lr : exec.issues().latestCopy())
                log(Level.none, ansi(lr.render(core.getTotalTests(), -1, null)), false);
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
        fileNewIssues(newIssues);
    }
}
