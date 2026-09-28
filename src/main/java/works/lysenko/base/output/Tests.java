package works.lysenko.base.output;

import org.apache.commons.lang3.StringUtils;
import works.lysenko.util.data.records.test.TestTime;
import works.lysenko.util.spec.Level;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.SPACE;
import static org.apache.commons.math3.util.FastMath.max;
import static works.lysenko.Base.core;
import static works.lysenko.Base.log;
import static works.lysenko.Base.section;
import static works.lysenko.base.output.File.fileTestsSummary;
import static works.lysenko.util.chrs.__.NO;
import static works.lysenko.util.chrs.____.PATH;
import static works.lysenko.util.chrs.____.TEST;
import static works.lysenko.util.chrs.____.WITH;
import static works.lysenko.util.data.enums.Ansi.yb;
import static works.lysenko.util.data.enums.Brackets.SQUARE;
import static works.lysenko.util.data.strs.Bind.b;
import static works.lysenko.util.data.strs.Case.c;
import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.data.strs.Swap.s1;
import static works.lysenko.util.data.strs.Wrap.e;
import static works.lysenko.util.func.type.Objects.isNotNull;
import static works.lysenko.util.lang.T.THERE_ARE;
import static works.lysenko.util.lang.word.C.COMMON;
import static works.lysenko.util.lang.word.C.COMPLETED;
import static works.lysenko.util.lang.word.S.SCENARIO;
import static works.lysenko.util.lang.word.S.SUMMARY;
import static works.lysenko.util.lang.word.T.TESTS;
import static works.lysenko.util.spec.Symbols.RHT_ARR;
import static works.lysenko.util.spec.Symbols._COLON_;
import static works.lysenko.util.spec.Symbols._DASH_;

/**
 * This class provides methods for finding common values among rows in a list.
 */
@SuppressWarnings({"StaticCollection", "AutoBoxing", "FeatureEnvy", "ForeachStatement", "AssignmentToMethodParameter",
        "NestedMethodCall", "ChainedMethodCall", "MethodWithMultipleLoops", "MethodCallInLoopCondition",
        "CallToSuspiciousStringMethod"})
record Tests() {

    private static final String TESTS_SUMMARY_PLACEHOLDER = s(_DASH_);
    private static List<String> common = null;
    private static List<List<String>> list = new ArrayList<>(1);
    private static int maxC = 0; // Max length of tests string
    private static int maxT = 0; // Max length of time string

    /**
     * Appends tests to a line based on common values and row data.
     *
     * @param common The list of common values among the rows.
     * @param row    The row to process.
     * @param line   The current line to append tests to.
     * @return The updated line with appended tests.
     */
    private static String appendTests(final List<String> common, final List<String> row, String line) {

        for (final String cell : row) {
            if (1 < row.indexOf(cell)) {
                final int index = row.indexOf(cell);
                if (index >= common.size() || null == common.get(index)) line = s(line, b(s(RHT_ARR), e(cell, true)));
                else line = s(line, b(s(RHT_ARR), e(TESTS_SUMMARY_PLACEHOLDER, true)));
            }
        }
        return line;
    }

    /**
     * Generates a summary of tests and outputs it to the console.
     * If there are no tests in the summary, it logs a message indicating no completed tests.
     * Otherwise, it logs the summary of tests, including common values and individual test details.
     */
    private static void consoleTestsSummary() {

        if (list.isEmpty()) log(Level.none, b(THERE_ARE, NO, COMPLETED, TESTS), true);
        else {
            final int tests = list.size();
            final int scenarios = list.stream().mapToInt(List::size).sum() - (tests << 1);
            section(b(s1(tests, c(TEST)), WITH, s1(scenarios, c(SCENARIO)), SUMMARY));
            common = define();
            process();
            summary();
        }
    }

    /**
     * This method returns a list of common values among the rows in the cyclesSummary list.
     *
     * @return A1 list of common values.
     */
    private static List<String> define() {

        defineCSwidths();
        return processAllRows(searchShortest());
    }

    /**
     * This method is used to calculate the maximum widths of two columns in the cyclesSummary list.
     * It loops through each row in the list and updates the maximum width values accordingly.
     */
    private static void defineCSwidths() {

        for (final List<String> row : list) {
            maxC = max(maxC, row.get(0).length());
            maxT = max(maxT, row.get(1).length());
        }
    }

    /**
     * Generates a single line of the summary details.
     *
     * @param common A1 list of common values among the rows in the cyclesSummary list.
     * @param row    Each row in the list to process.
     * @return The generated line for the summary.
     */
    private static String generateSummaryLine(final List<String> common, final List<String> row) {

        String line = EMPTY;
        final TestTime testTime = getTestTime(row);
        line = s(line, e(SQUARE, testTime.test()));
        line = s(line, e(e(SQUARE, testTime.time())));
        line = appendTests(common, row, line);
        return line;
    }

    /**
     * Retrieves TestTime object from a given row.
     *
     * @param row The row containing test and time values.
     * @return The TestTime object created from the row.
     */
    private static TestTime getTestTime(final List<String> row) {

        final String test = StringUtils.leftPad(row.get(0), maxC);
        final String time = StringUtils.leftPad(row.get(1), maxT);
        return new TestTime(test, time);
    }

    /**
     * Performs common processing for a given list of strings.
     */
    private static void process() {

        if (!common.isEmpty()) {
            while (!common.isEmpty() && null == common.get(common.size() - 1)) common.remove(common.size() - 1);
            final String add = (7 < (maxC + maxT)) ? SPACE.repeat((maxC + maxT) - 7) : EMPTY;
            final StringBuilder path = new StringBuilder(b(s(add, c(COMMON)), s(PATH, _COLON_, SPACE)));
            for (int i = 2; i < common.size(); i++)
                if (isNotNull(common.get(i))) path.append(b(s(RHT_ARR), s(common.get(i), SPACE)));
                else path.append(b(s(RHT_ARR), s(TESTS_SUMMARY_PLACEHOLDER, SPACE)));
            log(Level.none, yb(path.toString()), false);
        }
    }

    /**
     * This method processes all the rows in the cyclesSummary list to find common values among them.
     *
     * @param common The initial list of common values.
     * @return A1 list of common values among the rows.
     */
    private static List<String> processAllRows(final List<String> common) {

        for (final List<String> row : list) {
            for (final String cell : row) {
                final int index = row.indexOf(cell);
                if (index < common.size())
                    if (isNotNull(common.get(index))) if (!common.get(index).equals(cell)) common.set(index, null);
            }
        }
        return common;
    }

    /**
     * Executes the run method.
     * <p>
     * This method retrieves a tests summary list from the core.getCyclesSummary() method.
     * It then calls the consoleCyclesSummary() method to generate a summary of tests and output it to the console.
     * Next, it calls the fileCyclesSummary() method to generate a summary of tests and output it to a file.
     */
    static void run() {

        list = core.getTestsSummary();
        consoleTestsSummary();
        fileTestsSummary(list);
    }

    /**
     * This method searches for the shortest row in the cyclesSummary list and returns it as a list of strings.
     *
     * @return The shortest row in the cyclesSummary list as a list of strings.
     */
    private static List<String> searchShortest() {

        return new ArrayList<>(list.stream() // making a copy of data
                .min(Comparator.comparingInt(List::size))
                .orElse(Collections.emptyList()));
    }

    /**
     * Generates a summary of tests and outputs it to the console.
     * If there are no tests in the summary, it logs a message indicating no completed tests.
     * Otherwise, it logs the summary of tests, including common values and individual test details.
     */
    private static void summary() {

        for (final List<String> row : list) {
            final String summaryLine = generateSummaryLine(common, row);
            log(Level.none, summaryLine, false);
        }
    }
}
