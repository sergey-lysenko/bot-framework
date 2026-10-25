package works.lysenko.base.test;


import works.lysenko.base.Core;
import works.lysenko.base.output.Reports;
import works.lysenko.base.output.LogHtml;
import works.lysenko.util.apis.core._Reports;
import works.lysenko.util.apis.test._Stat;

import java.util.ArrayList;
import java.util.List;

import static works.lysenko.Base.section;
import static works.lysenko.base.output.Json.jsonStats;
import static works.lysenko.base.output.TreeHtml.treeStats;
import static works.lysenko.util.chrs.__.IN;
import static works.lysenko.util.chrs.____.JSON;
import static works.lysenko.util.lang.word.T.TREE;
import static works.lysenko.util.data.strs.Bind.b;
import static works.lysenko.util.data.strs.Case.c;
import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.func.core.Assertions.assertEqualsSilent;
import static works.lysenko.util.lang.word.D.DISCREPANCY;
import static works.lysenko.util.lang.word.H.HISTORY;
import static works.lysenko.util.lang.word.T.TESTS;

/**
 * The Stat class is responsible for collecting and displaying statistics related to tests.
 */
@SuppressWarnings({"AssignmentOrReturnOfFieldWithMutableType", "AutoBoxing", "NestedMethodCall", "ObjectAllocationInLoop",
        "ValueOfIncrementOrDecrementUsed", "ForeachStatement"})
public class Stat implements _Stat {

    private static final _Reports reports = new Reports();
    private final List<List<String>> history = new ArrayList<>(1);
    private final List<Long> times = new ArrayList<>(1);

    /**
     * Executes the stats method which displays statistics.
     * <p>
     * This method calls the stats method of the {@link Reports} object obtained from the {@link Core} object.
     * It also calls several other internal methods to generate different statistics.
     */
    public static void reports() {

        if (null != works.lysenko.Base.timer) {
            works.lysenko.base.output.EtaCorrection.recordRunCompletion(works.lysenko.Base.timer.msSinceStart());
        }
        reports.run();
        section(JSON);
        jsonStats();
        section(TREE);
        treeStats();
        LogHtml.logStats();
    }

    /**
     * Returns the execution history list of lists.
     *
     * @return execution history
     */
    public final List<List<String>> history() {

        return history;
    }

    /**
     * Returns the most recent execution history list, or null if empty.
     *
     * @return recent execution history or null
     */
    public final List<String> previous() {

        final List<List<String>> the = List.copyOf(history);
        return (the.isEmpty()) ? null : the.get(the.size() - 1);
    }

    /**
     * Generates a summary table of execution times and history.
     *
     * @return summary table list of rows
     */
    public final List<List<String>> summary() {

        final List<List<String>> testsSummary = new ArrayList<>(0);
        assertEqualsSilent(history.size(), times.size(),
                b(c(DISCREPANCY), IN, c(TESTS), c(HISTORY)));
        int index = 0;
        for (final List<String> list : history) {
            final List<String> row = new ArrayList<>(1);
            row.add(s(index + 1));
            row.add(String.format("%,d", times.get(index++))); //NON-NLS
            row.addAll(list);
            testsSummary.add(row);
        }
        return testsSummary;
    }

    /**
     * Returns the list of execution times.
     *
     * @return execution times
     */
    public final List<Long> times() {

        return times;
    }
}
