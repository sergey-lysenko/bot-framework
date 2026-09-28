package works.lysenko.base.output;

import works.lysenko.util.apis.data._Result;
import works.lysenko.util.apis.log._LogRecord;
import works.lysenko.util.data.enums.EventType;
import works.lysenko.util.spec.Layout;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static works.lysenko.Base.core;
import static works.lysenko.Base.isDebug;
import static works.lysenko.util.chrs.__.OF;
import static works.lysenko.util.chrs.___.HAD;
import static works.lysenko.util.chrs.___.NEW;
import static works.lysenko.util.chrs.___.SET;
import static works.lysenko.util.chrs.____.TIME;
import static works.lysenko.util.chrs.____.WERE;
import static works.lysenko.util.chrs.____.WITH;
import static works.lysenko.util.data.enums.Brackets.ROUND;
import static works.lysenko.util.data.enums.Brackets.SQUARE;
import static works.lysenko.util.data.strs.Bind.b;
import static works.lysenko.util.data.strs.Case.c;
import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.data.strs.Wrap.e;
import static works.lysenko.util.lang.T.TO_BE;
import static works.lysenko.util.lang.word.A.AMONG;
import static works.lysenko.util.lang.word.C.CHANCE;
import static works.lysenko.util.lang.word.C.CURRENT;
import static works.lysenko.util.lang.word.E.EXECUTED;
import static works.lysenko.util.lang.word.I.ISSUE;
import static works.lysenko.util.lang.word.I.ISSUES;
import static works.lysenko.util.lang.word.P.PATHS;
import static works.lysenko.util.lang.word.P.POSSIBLE;
import static works.lysenko.util.lang.word.R.RESULT;
import static works.lysenko.util.lang.word.S.*;
import static works.lysenko.util.lang.word.T.TESTS;
import static works.lysenko.util.lang.word.T.THESE;
import static works.lysenko.util.spec.Html.*;
import static works.lysenko.util.spec.Layout.Templates.*;
import static works.lysenko.util.spec.Symbols.A;
import static works.lysenko.util.spec.Symbols._NUMBR_;

/**
 * Files output routines
 */
@SuppressWarnings({"AutoBoxing", "ChainedMethodCall", "LawOfDemeter", "StaticMethodOnlyUsedInOneClass",
        "MethodWithMultipleLoops",
        "NestedMethodCall", "ForeachStatement", "FeatureEnvy"})
record File() {

    /**
     * @param newIssues reference
     */
    @SuppressWarnings("NestedMethodCall")
    static void fileNewIssues(final Collection<? extends _LogRecord> newIssues) {

        if (isDebug()) writeIssuesFile(Layout.Files.name(NEW_ISSUES_NOTICE_), newIssues);
        final Collection<_LogRecord> noNotices = new ArrayList<>(0);
        for (final _LogRecord row : newIssues) {
            if (!row.text().contains(EventType.NOTICE.getString())) noNotices.add(row);
        }
        writeIssuesFile(Layout.Files.name(NEW_ISSUES_WARNING_), noNotices);
    }

    /**
     * @param total      total
     * @param active     active
     * @param percentage percentage
     * @param sorted     sorted
     */
    static void fileScenariosStatistics(final int total, final int active, final String percentage,
                                        final Map<String, _Result> sorted) {

        final String name = Layout.Files.name(SCENARIOS_STATISTICS_);
        final Collection<String> lines = new ArrayList<>(1);
        if (!sorted.isEmpty()) {
            lines.add(s(HTML_HEAD_TITLE_, name, _TITLE_HEAD, BODY_));
            lines.add(s(H_1_, name, _H_1));
            lines.add(s(H_2_, b(c(SCENARIOS), STATISTICS), _H_2));
            lines.add(s(TABLE_, TR_, TD_));
            lines.add(b(s(total), PATHS, WERE, POSSIBLE, WITH, CURRENT, SET, OF, c(SCENARIOS)));
            lines.add(s(_TD, _TR, TR_, TD_));
            lines.add(b(s(active), e(ROUND, percentage), AMONG, THESE, HAD, s(A), CHANCE, TO_BE, EXECUTED));
            lines.add(s(_TD, _TR, _TABLE));
            lines.add(s(TABLE_TR_TH_, _TH_TH_, c(SCENARIO), _TH_TH_, c(RESULT), _TH_TR));
            for (final Map.Entry<String, _Result> row : sorted.entrySet()) {
                lines.add(s(TR_, TD_));
                lines.add(row.getValue().getScenarioType().tag());
                lines.add(s(_TD, TD_));
                lines.add(row.getKey());
                lines.add(s(_TD, TD_));
                lines.add(row.getValue().toString());
                lines.add(s(_TD, _TR));
            }
            lines.add(_TABLE_BODY_HTML);
            works.lysenko.util.func.type.Files.writeToFile(null, lines, name);
        }
    }

    /**
     * @param testsSummary reference
     */
    static void fileTestsSummary(final Collection<? extends List<String>> testsSummary) {

        final String name = Layout.Files.name(TESTS_HISTORY_);
        final Collection<String> lines = new ArrayList<>(1);
        if (!testsSummary.isEmpty()) {
            lines.add(s(HTML_HEAD_TITLE_, name, _TITLE_HEAD, BODY_));
            lines.add(s(H_1_, name, _H_1));
            lines.add(b(H_2_, c(TESTS), s(SUMMARY), _H_2));
            lines.add(s(TABLE_TR_TH_, _NUMBR_, _TH_TH_, c(TIME), _TH_TH_, c(SCENARIOS), _TH_TR));
            for (final List<String> row : testsSummary) {
                lines.add(TR_);
                for (final String data : row)
                    lines.add(s(TD_, e(SQUARE, data), _TD));
                lines.add(_TR);
            }
            lines.add(_TABLE_BODY_HTML);
            works.lysenko.util.func.type.Files.writeToFile(null, lines, name);
        }
    }

    private static void writeIssuesFile(final String name, final Collection<? extends _LogRecord> records) {

        final Collection<String> lines = new ArrayList<>(1);
        if (!records.isEmpty()) {
            lines.add(s(HTML_HEAD_TITLE_, name, _TITLE_HEAD, BODY_));
            lines.add(s(H_1_, name, _H_1));
            lines.add(b(H_2_, c(NEW), s(ISSUES), _H_2));
            lines.add(s(TABLE_TR_TH_, c(ISSUE), _TH_TR));
            for (final _LogRecord row : records) {
                lines.add(TR_);
                lines.add(s(TD_, row.renderTest(core.getTotalTests()), _TD));
                lines.add(s(TD_, row.renderTime(core.getLogger().getSpanLength()), _TD));
                lines.add(s(TD_, "&nbsp;".repeat(row.data().depth()), row.text(), _TD));
                lines.add(_TR);
            }
            lines.add(_TABLE_BODY_HTML);
            works.lysenko.util.func.type.Files.writeToFile(null, lines, name);
        }
    }
}
