package works.lysenko.base.output;

import org.apache.commons.math3.fraction.Fraction;
import org.junit.jupiter.api.Test;
import works.lysenko.base.output.loghtml.LogModels.ScenEntry;
import works.lysenko.util.data.enums.ExecutionStatus;
import works.lysenko.util.data.enums.ScenarioType;
import works.lysenko.util.data.type.LogRecord;
import works.lysenko.util.data.type.Result;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SummaryPlaqueTest {

    @Test
    void testFormatPct() {
        assertEquals("0%", SummaryPlaque.formatPct(0, 0));
        assertEquals("0%", SummaryPlaque.formatPct(0, 10));
        assertEquals("100%", SummaryPlaque.formatPct(10, 10));
        assertEquals("50.0%", SummaryPlaque.formatPct(5, 10));
        assertEquals("33.3%", SummaryPlaque.formatPct(1, 3));
    }

    @Test
    void testComputeFromScenEntriesAndRenderHtml() {
        final List<ScenEntry> entries = new ArrayList<>();
        entries.add(new ScenEntry("▷", "Scenario1", "1.0", "1", ""));
        entries.add(new ScenEntry("▷", "Scenario2", "1.0", "1", "warn"));
        entries.add(new ScenEntry("▷", "Scenario3", "1.0", "1", "fail"));
        entries.add(new ScenEntry("▷", "Scenario4", "1.0", "1", "child_failed"));
        entries.add(new ScenEntry("▷", "Scenario5", "0.0", "0", ""));

        final SummaryPlaque plaque = SummaryPlaque.computeFromScenEntries(entries);
        assertEquals(5, plaque.total());
        assertEquals(4, plaque.executable());
        assertEquals(4, plaque.executed());
        assertEquals(1, plaque.passed());
        assertEquals(1, plaque.warning());
        assertEquals(2, plaque.failed());

        final String html = plaque.renderHtml();
        assertNotNull(html);
        assertTrue(html.contains("Amount"));
        assertTrue(html.contains("Percentage"));
        assertFalse(html.contains("<th class=\"col-total\">Total</th>"));
        assertFalse(html.contains("<th class=\"col-executable\">Executable</th>"));
        assertTrue(html.contains("Executed"));
        assertTrue(html.contains("Passed"));
        assertTrue(html.contains("Warning"));
        assertTrue(html.contains("Failed"));
        assertTrue(html.contains("20.0%"));
    }

    @Test
    void testComputeFromNodesAndRenderHtmlWithUpset() {
        final List<TreeHtml.NodeData> nodes = new ArrayList<>();

        // Node 1: passed
        final Result r1 = new Result(ScenarioType.LEAF, Fraction.ONE, Fraction.ONE, Fraction.ONE, List.of(), 1, 1);
        nodes.add(new TreeHtml.NodeData("n1", "Scenario1", "Tests", 0, 1.0, r1));

        // Node 2: warning (with an event)
        final LogRecord dummyLog = new LogRecord(1, System.currentTimeMillis(), null);
        final Result r2 = new Result(ScenarioType.LEAF, Fraction.ONE, Fraction.ONE, Fraction.ONE, List.of(dummyLog), 1, 1);
        nodes.add(new TreeHtml.NodeData("n2", "Scenario2", "Tests", 0, 2.0, r2));

        // Node 3: upset (child failure downstream)
        final Result r3 = new Result(ScenarioType.NODE, Fraction.ONE, Fraction.ONE, Fraction.ONE, List.of(), 1, 1);
        r3.updateStatus(ExecutionStatus.CHILD_FAILED);
        nodes.add(new TreeHtml.NodeData("n3", "Scenario3", "Tests", 0, 3.0, r3));

        // Node 4: failed (direct failure)
        final Result r4 = new Result(ScenarioType.LEAF, Fraction.ONE, Fraction.ONE, Fraction.ONE, List.of(), 1, 1);
        r4.updateStatus(ExecutionStatus.FAILED);
        nodes.add(new TreeHtml.NodeData("n4", "Scenario4", "Tests", 0, 4.0, r4));

        // Node 5: unvisited
        final Result r5 = new Result(ScenarioType.LEAF, Fraction.ONE, Fraction.ONE, Fraction.ONE, List.of(), 0, 1);
        nodes.add(new TreeHtml.NodeData("n5", "Scenario5", "Tests", 0, 5.0, r5));

        final SummaryPlaque.RunConfig config = new SummaryPlaque.RunConfig("SuiteA", "PoolB", "http://domain", "EXHAUSTIVE", "10 / 12 / 20");
        final SummaryPlaque plaque = SummaryPlaque.computeFromNodes(nodes, config);

        assertEquals(5, plaque.total());
        assertEquals(5, plaque.executable());
        assertEquals(4, plaque.executed());
        assertEquals(0, plaque.off());
        assertEquals(1, plaque.notExecuted());
        assertEquals(0, plaque.hidden());
        assertEquals(1, plaque.passed());
        assertEquals(0, plaque.notice());
        assertEquals(1, plaque.warning());
        assertEquals(1, plaque.upset());
        assertEquals(1, plaque.failed());
        assertEquals(plaque.total(),
                plaque.off() + plaque.notExecuted() + plaque.hidden() + plaque.passed()
                        + plaque.notice() + plaque.warning() + plaque.upset() + plaque.failed(),
                "All mutually exclusive state buckets must sum to Total");

        final String html = plaque.renderHtml();
        assertNotNull(html);

        // Header and config preserved
        assertTrue(html.contains("SuiteA"));
        assertTrue(html.contains("PoolB"));
        assertTrue(html.contains("http://domain"));
        assertTrue(html.contains("EXHAUSTIVE"));
        assertTrue(html.contains("10 / 12 / 20"));

        // Non-zero partition columns shown; zero-value and subtotal columns omitted
        assertTrue(html.contains("<th class=\"col-total\">Total</th>"));
        assertTrue(html.contains("<th class=\"col-not-executed\""));
        assertTrue(html.contains("<th class=\"col-passed\" title=\"Visible scenario nodes that passed cleanly\">Passed Nodes</th>"));
        assertTrue(html.contains("<th class=\"col-warning\">Warning</th>"));
        assertTrue(html.contains("<th class=\"col-upset\" title=\"Scenarios whose execution passed, but a child failed downstream\">Upset</th>"));
        assertTrue(html.contains("<th class=\"col-failed\">Failed</th>"));

        assertFalse(html.contains("<th class=\"col-executable\">Executable</th>"), "Subtotal Executable must not be shown");
        assertFalse(html.contains("<th class=\"col-executed\">Executed</th>"), "Subtotal Executed must not be shown");
        assertFalse(html.contains("<th class=\"col-off\""), "Zero-valued Not Executable column must be hidden");
        assertFalse(html.contains("<th class=\"col-hidden\""), "Zero-valued Hidden column must be hidden");
        assertFalse(html.contains("<th class=\"col-notice\""), "Zero-valued Notice column must be hidden");

        // Amount row checks
        assertTrue(html.contains("<td class=\"col-total\">5</td>"));
        assertTrue(html.contains("<td class=\"col-not-executed\">1</td>"));
        assertTrue(html.contains("<td class=\"stat-passed\">1</td>"));
        assertTrue(html.contains("<td class=\"stat-warning\">1</td>"));
        assertTrue(html.contains("<td class=\"stat-upset\">1</td>"));
        assertTrue(html.contains("<td class=\"stat-failed\">1</td>"));
    }

    @Test
    void testAllStatesPartitionTotalAndHideZerosInHtmlAndSvg() {
        final List<TreeHtml.NodeData> nodes = new ArrayList<>();

        // 1. Not Executable (weight 0, 0 execs)
        final Result rOff = new Result(ScenarioType.LEAF, Fraction.ZERO, Fraction.ZERO, Fraction.ZERO, List.of(), 0, 1);
        final TreeHtml.NodeData nOff = new TreeHtml.NodeData("nOff", "OffLeaf", "Tests", 0, 1.0, rOff);
        nodes.add(nOff);

        // 2. Not Executed (weight 1, 0 execs)
        final Result rUnvisited = new Result(ScenarioType.LEAF, Fraction.ONE, Fraction.ONE, Fraction.ONE, List.of(), 0, 1);
        final TreeHtml.NodeData nUnvisited = new TreeHtml.NodeData("nUnvisited", "UnvisitedLeaf", "Tests", 0, 2.0, rUnvisited);
        nodes.add(nUnvisited);

        // 3. Hidden (passed leaf marked hidden)
        final Result rHidden = new Result(ScenarioType.LEAF, Fraction.ONE, Fraction.ONE, Fraction.ONE, List.of(), 1, 1);
        final TreeHtml.NodeData nHidden = new TreeHtml.NodeData("nHidden", "HiddenLeaf", "Tests", 0, 3.0, rHidden);
        nodes.add(nHidden);

        // 4. Passed (visible passed)
        final Result rPassed = new Result(ScenarioType.LEAF, Fraction.ONE, Fraction.ONE, Fraction.ONE, List.of(), 1, 1);
        final TreeHtml.NodeData nPassed = new TreeHtml.NodeData("nPassed", "PassedLeaf", "Tests", 0, 4.0, rPassed);
        nodes.add(nPassed);

        // 5. Notice (S3 notice event)
        final works.lysenko.util.data.type.logr.type.Event noticeEv = new works.lysenko.util.data.type.logr.type.Event(
                1, works.lysenko.util.data.enums.EventType.NOTICE, 2, "Minor notice", "");
        final Result rNotice = new Result(ScenarioType.LEAF, Fraction.ONE, Fraction.ONE, Fraction.ONE,
                List.of(new LogRecord(1, System.currentTimeMillis(), noticeEv)), 1, 1);
        final TreeHtml.NodeData nNotice = new TreeHtml.NodeData("nNotice", "NoticeLeaf", "Tests", 0, 5.0, rNotice);
        nodes.add(nNotice);

        // 6. Warning (S2 warning event)
        final works.lysenko.util.data.type.logr.type.Event warnEv = new works.lysenko.util.data.type.logr.type.Event(
                1, works.lysenko.util.data.enums.EventType.WARNING, 2, "Some warning", "");
        final Result rWarn = new Result(ScenarioType.LEAF, Fraction.ONE, Fraction.ONE, Fraction.ONE,
                List.of(new LogRecord(1, System.currentTimeMillis(), warnEv)), 1, 1);
        final TreeHtml.NodeData nWarn = new TreeHtml.NodeData("nWarn", "WarnLeaf", "Tests", 0, 6.0, rWarn);
        nodes.add(nWarn);

        // 7. Upset (child failed)
        final Result rUpset = new Result(ScenarioType.NODE, Fraction.ONE, Fraction.ONE, Fraction.ONE, List.of(), 1, 1);
        rUpset.updateStatus(ExecutionStatus.CHILD_FAILED);
        final TreeHtml.NodeData nUpset = new TreeHtml.NodeData("nUpset", "UpsetNode", "Tests", 0, 7.0, rUpset);
        nodes.add(nUpset);

        // 8. Failed (direct failure, even with 0 completed execs)
        final Result rFailed = new Result(ScenarioType.LEAF, Fraction.ONE, Fraction.ONE, Fraction.ONE, List.of(), 0, 1);
        rFailed.updateStatus(ExecutionStatus.FAILED);
        final TreeHtml.NodeData nFailed = new TreeHtml.NodeData("nFailed", "FailedLeaf", "Tests", 0, 8.0, rFailed);
        nodes.add(nFailed);

        final SummaryPlaque.RunConfig config = new SummaryPlaque.RunConfig("E2E", "local", "http://localhost", "EXHAUSTIVE", null);
        final SummaryPlaque plaque = SummaryPlaque.computeFromNodes(
                nodes, config, new TreeHtml.AccessibleContext(java.util.Set.of()),
                new java.util.HashMap<>(), new java.util.HashMap<>(), java.util.Set.of(nHidden));

        assertEquals(8, plaque.total());
        assertEquals(1, plaque.off());
        assertEquals(1, plaque.notExecuted());
        assertEquals(1, plaque.hidden());
        assertEquals(1, plaque.passed());
        assertEquals(1, plaque.notice());
        assertEquals(1, plaque.warning());
        assertEquals(1, plaque.upset());
        assertEquals(1, plaque.failed());

        final List<SummaryPlaque.MetricColumn> cols = plaque.visibleTreeColumns();
        assertEquals(9, cols.size(), "Total + all 8 non-zero states must be visible");
        final int restSum = cols.stream().skip(1).mapToInt(SummaryPlaque.MetricColumn::amount).sum();
        assertEquals(plaque.total(), restSum, "Sum of all non-Total columns must equal Total");

        final String html = plaque.renderHtml();
        for (final String expected : List.of("Total", "Not Executable", "Not Executed", "Hidden Leaves", "Passed Nodes", "Notice", "Warning", "Upset", "Failed")) {
            assertTrue(html.contains(">" + expected + "</th>"), "HTML must contain column: " + expected);
        }

        final TreeHtml.TreeLayout layout = new TreeHtml.TreeLayout(nodes, List.of(), plaque);
        final String svg = works.lysenko.base.output.tree.SvgTreeRenderer.renderTreeProgressionSvg(
                layout, 1, "Test #1", java.util.Set.of(), java.util.Set.of(),
                new TreeHtml.AccessibleContext(java.util.Set.of()), null, null, 0, 0);
        for (final String expected : List.of("TOTAL", "NOT EXECUTABLE", "NOT EXECUTED", "HIDDEN LEAVES", "PASSED NODES", "NOTICE", "WARNING", "UPSET", "FAILED")) {
            assertTrue(svg.contains(">" + expected + "</text>"), "SVG must contain column: " + expected);
        }
        assertTrue(svg.contains("fill=\"#94A3B8\" font-weight=\"normal\">SUITE:</tspan>"));
        assertTrue(svg.contains("fill=\"#F8FAFC\" font-weight=\"bold\">E2E</tspan>"));
        assertTrue(svg.contains(">EXHAUSTIVE</text>"));
        assertTrue(svg.contains("width=\"1168\""), "Progression frame plaque must span full frame width (1200 - 32)");
    }

    @Test
    void testPrunedLayoutPreservesTotalAndHiddenInStaticHtmlAndProgressionSnapshot() {
        final boolean origBranches = works.lysenko.util.prop.tree.HideOff.branches;
        final boolean origPassedLeaves = works.lysenko.util.prop.tree.HideOff.passedLeaves;
        try {
            works.lysenko.util.prop.tree.HideOff.branches = true;
            works.lysenko.util.prop.tree.HideOff.passedLeaves = true;

            final java.util.TreeMap<String, Result> sorted = new java.util.TreeMap<>();
            sorted.put("Root", new Result(ScenarioType.NODE, Fraction.ONE, Fraction.ONE, Fraction.ONE, List.of(), 1, 1));
            sorted.put("root.PassedLeaf", new Result(ScenarioType.LEAF, Fraction.ONE, Fraction.ONE, Fraction.ONE, List.of(), 1, 1));
            sorted.put("root.OffLeaf", new Result(ScenarioType.LEAF, Fraction.ZERO, Fraction.ZERO, Fraction.ZERO, List.of(), 0, 1));
            sorted.put("root.PendingLeaf", new Result(ScenarioType.LEAF, Fraction.ONE, Fraction.ONE, Fraction.ONE, List.of(), 0, 1));

            final TreeHtml.TreeLayout layout = TreeHtml.computeLayout(sorted);
            assertNotNull(layout.plaque());
            assertEquals(4, layout.plaque().total(), "Total must reflect all 4 unpruned nodes");
            assertEquals(2, layout.nodes().size(), "OffLeaf and PassedLeaf must be pruned from visible layout");
            assertEquals(1, layout.plaque().off());
            assertEquals(1, layout.plaque().hidden());
            assertEquals(1, layout.plaque().passed());
            assertEquals(1, layout.plaque().notExecuted());
            assertEquals(0, layout.plaque().warning());
            assertEquals(0, layout.plaque().failed());

            // Final static HTML must keep Total=4, show Not Executable / Not Executed / Hidden Leaves / Passed Nodes, and omit zeros
            final String staticHtml = TreeHtml.renderHtml(layout);
            assertTrue(staticHtml.contains("<td class=\"col-total\">4</td>"));
            assertTrue(staticHtml.contains("<th class=\"col-off\""));
            assertTrue(staticHtml.contains("<th class=\"col-hidden\""));
            assertTrue(staticHtml.contains(">Hidden Leaves</th>"));
            assertTrue(staticHtml.contains("<th class=\"col-not-executed\""));
            assertTrue(staticHtml.contains(">Passed Nodes</th>"));
            assertFalse(staticHtml.contains("<th class=\"col-warning\">Warning</th>"));
            assertFalse(staticHtml.contains("<th class=\"col-failed\">Failed</th>"));

            // Progression snapshot must preserve layout.plaque() and render identically in SVG
            final TreeHtml.TreeLayout snapshot = TreeHtml.snapshotLayout(layout, new TreeHtml.AccessibleContext(java.util.Set.of()));
            assertSame(layout.plaque(), snapshot.plaque(), "snapshotLayout must preserve the precomputed SummaryPlaque");
            final String svg = works.lysenko.base.output.tree.SvgTreeRenderer.renderTreeProgressionSvg(
                    snapshot, 1, "Test #1", java.util.Set.of(), java.util.Set.of(),
                    new TreeHtml.AccessibleContext(java.util.Set.of()), null, null, 0, 0);
            assertTrue(svg.contains(">TOTAL</text>"));
            assertTrue(svg.contains(">4</text>"));
            assertTrue(svg.contains(">NOT EXECUTABLE</text>"));
            assertTrue(svg.contains(">NOT EXECUTED</text>"));
            assertTrue(svg.contains(">HIDDEN LEAVES</text>"));
            assertTrue(svg.contains(">PASSED NODES</text>"));
            assertFalse(svg.contains(">WARNING</text>"));
            assertFalse(svg.contains(">FAILED</text>"));
            assertFalse(svg.contains(">UPSET</text>"));
            assertFalse(svg.contains(">NOTICE</text>"));
        } finally {
            works.lysenko.util.prop.tree.HideOff.branches = origBranches;
            works.lysenko.util.prop.tree.HideOff.passedLeaves = origPassedLeaves;
        }
    }

    @Test
    void testRunConfigParsingAndRendering() {
        final String logLine = "[  ][37][.795][  2] Executing tests of Development with development-local on https://stg-dashboard.myordering.online in EXHAUSTIVE mode";
        final SummaryPlaque.RunConfig config = SummaryPlaque.RunConfig.parse(logLine);

        assertEquals("Development", config.suite());
        assertEquals("development-local", config.pool());
        assertEquals("https://stg-dashboard.myordering.online", config.domain());
        assertEquals("EXHAUSTIVE", config.mode());

        final List<ScenEntry> entries = new ArrayList<>();
        entries.add(new ScenEntry("▷", "Scenario1", "1.0", "1", ""));

        final SummaryPlaque plaque = SummaryPlaque.computeFromScenEntries(entries, logLine, "14 / 17 / 48 (82.4% executed)");
        final String html = plaque.renderHtml();

        assertTrue(html.contains("Development"), "Plaque HTML must contain test suite name");
        assertTrue(html.contains("development-local"), "Plaque HTML must contain pool name");
        assertTrue(html.contains("https://stg-dashboard.myordering.online"), "Plaque HTML must contain domain URL");
        assertTrue(html.contains("14 / 17 / 48"), "Plaque HTML must contain path summary");
        assertTrue(html.contains("EXHAUSTIVE"), "Plaque HTML must contain mode badge");
    }

    @Test
    void testComputeFromSections() {
        final List<works.lysenko.base.output.loghtml.LogModels.LogSection> sections = new ArrayList<>();
        sections.add(new works.lysenko.base.output.loghtml.LogModels.LogSection("booting", "Booting", "neutral", null));

        // 6 passed tests
        for (int i = 1; i <= 6; i++) {
            final works.lysenko.base.output.loghtml.LogModels.LogSection sec =
                    new works.lysenko.base.output.loghtml.LogModels.LogSection("test", "Test #" + i, "passed", null);
            sec.testNum = i;
            sections.add(sec);
        }

        // 2 warning tests
        for (int i = 7; i <= 8; i++) {
            final works.lysenko.base.output.loghtml.LogModels.LogSection sec =
                    new works.lysenko.base.output.loghtml.LogModels.LogSection("test", "Test #" + i, "warning", null);
            sec.testNum = i;
            sections.add(sec);
        }

        // 8 failed tests
        for (int i = 9; i <= 16; i++) {
            final works.lysenko.base.output.loghtml.LogModels.LogSection sec =
                    new works.lysenko.base.output.loghtml.LogModels.LogSection("test", "Test #" + i, "failed", null);
            sec.testNum = i;
            sections.add(sec);
        }

        sections.add(new works.lysenko.base.output.loghtml.LogModels.LogSection("postflight", "Postflight", "neutral", null));

        final String logLine = "Executing tests of Development with development-local on https://stg-dashboard.myordering.online in EXHAUSTIVE mode";
        final SummaryPlaque plaque = SummaryPlaque.computeFromSections(sections, logLine, "14 / 17 / 48");

        assertEquals(16, plaque.total());
        assertEquals(16, plaque.executable());
        assertEquals(16, plaque.executed());
        assertEquals(6, plaque.passed());
        assertEquals(2, plaque.warning());
        assertEquals(8, plaque.failed());

        assertEquals("100%", SummaryPlaque.formatPct(plaque.executed(), plaque.total()));
        assertEquals("37.5%", SummaryPlaque.formatPct(plaque.passed(), plaque.total()));
        assertEquals("12.5%", SummaryPlaque.formatPct(plaque.warning(), plaque.total()));
        assertEquals("50.0%", SummaryPlaque.formatPct(plaque.failed(), plaque.total()));

        final String html = plaque.renderHtml();
        assertNotNull(html);
        assertTrue(html.contains("col-executed\">16</td>"));
        assertTrue(html.contains("col-passed\">6</td>"));
        assertTrue(html.contains("col-warning\">2</td>"));
        assertTrue(html.contains("col-failed\">8</td>"));
        assertTrue(html.contains("37.5%"));
        assertTrue(html.contains("12.5%"));
        assertTrue(html.contains("50.0%"));
    }

    @Test
    void testCss() {
        final String css = SummaryPlaque.css();
        assertNotNull(css);
        assertTrue(css.contains(".summary-plaque-card { width: fit-content; max-width: 100%;"));
        assertTrue(css.contains(".col-failed"));
        assertTrue(css.contains(".col-upset"));
        assertTrue(css.contains(".col-upset, .summary-plaque-card .stat-upset { color: #818cf8;"));
        assertTrue(css.contains(".col-total"));
        assertTrue(css.contains(".col-executable"));
    }
}
