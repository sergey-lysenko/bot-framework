package works.lysenko.base.output;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LogHtmlTest {

    @BeforeEach
    @AfterEach
    void resetState() {
        if (works.lysenko.util.func.type.Objects.isNotNull(works.lysenko.Base.properties)) {
            works.lysenko.Base.properties.clearUserOverrides();
        }
        works.lysenko.base.output.EtaCorrection.reset();
    }

    @Test
    void testLimboAndPreflightParsedWithPaddedBrackets(@TempDir final Path tempDir) throws IOException {
        final Path logPath = tempDir.resolve("sample.run.log");
        final Path htmlPath = tempDir.resolve("sample.run.log.html");

        final List<String> logLines = List.of(
                "Starting Bot core ...",
                "# Applied test configuration",
                "[ ][1][.010][9] Initializing Driver ...",
                "[ 1][2][1.000][10] Executing Scenario A",
                "[ 1][3][1.050][5] • Closing test 1 ...",
                "[ ][4][1.060][2] • Test time 50 ms",
                "[ ][5][1.065][1] Persisting Test history",
                "[ 2][6][2.000][10] Executing Scenario B",
                "[ 2][7][2.050][5] • Closing test 2 ...",
                "[ ][8][2.060][2] • Test time 50 ms",
                "[ ][9][2.070][1] 2 tests of Sample done in 2 s",
                "[ ][10][2.080][1] = 1 Event summary =",
                "[ ][11][2.090][1] Common path: → StepA",
                "[ ][12][2.095][0] [ 1] [1,050] → - → StepA",
                "[ ][13][2.098][0] [ 2] [1,050] → - → StepB",
                "[ ][14][2.100][1] 37 paths were possible with current set of Scenarios",
                "[ ][15][2.101][0] 37 (100.0%) among these had a chance to be executed",
                "[ ][16][2.102][0] 35 (94.6%) among these were actually executed"
        );

        Files.write(logPath, logLines);
        LogHtml.generateReport(logPath.toFile(), htmlPath.toFile());

        assertTrue(Files.exists(htmlPath), "HTML report should be generated");
        final String html = Files.readString(htmlPath);

        // Verify limbo sections and bars exist
        assertTrue(html.contains("class=\"log-section limbo"), "Should render limbo log section");
        assertTrue(html.contains("class=\"bar-col limbo\""), "Should render timeline limbo bar");
        assertTrue(html.contains("stat-group-limbo"), "Should render limbo telemetry stats");

        // Verify preflight and postflight sections
        assertTrue(html.contains("class=\"log-section preflight"), "Should render preflight section");
        assertTrue(html.contains("class=\"log-section postflight"), "Should render postflight section");

        // Verify execution paths with space-padded test numbers
        assertTrue(html.contains("2 Tests Executed"), "Should recognize both space-padded execution paths");

        // Verify scenario statistics subtitle contains all 3 lines
        assertTrue(html.contains("37 paths were possible with current set of Scenarios"), "Should include paths possible string in HTML");
        assertTrue(html.contains("37 (100.0%) among these had a chance to be executed"), "Should include paths chance string in HTML");
        assertTrue(html.contains("35 (94.6%) among these were actually executed"), "Should include paths executed string in HTML");
    }

    @Test
    void testLineGraphRenderedForMoreThan50Cycles(@TempDir final Path tempDir) throws IOException {
        final Path logPath = tempDir.resolve("many_cycles.run.log");
        final Path htmlPath = tempDir.resolve("many_cycles.run.log.html");

        final List<String> logLines = new ArrayList<>();
        logLines.add("Starting Bot core ...");
        logLines.add("# Applied test configuration");

        for (int i = 1; i <= 60; i++) {
            final double t0 = (i - 1) * 2.0;
            final double t1 = t0 + 1.5;
            final double tLimboEnd = t0 + 2.0;
            logLines.add(String.format("[%3d][%d][%.3f][10] Executing Scenario Test%d", i, (i * 4), t0, i));
            logLines.add(String.format("[%3d][%d][%.3f][5] • Closing test %d ...", i, (i * 4 + 1), t1, i));
            logLines.add(String.format("[   ][%d][%.3f][2] • Test time 1 s 500 ms", (i * 4 + 2), t1 + 0.05));
            logLines.add(String.format("[   ][%d][%.3f][1] Persisting Test history", (i * 4 + 3), tLimboEnd));
        }
        logLines.add("[   ][300][122.000][1] 60 tests of LargeSuite done in 2 m 2 s");

        Files.write(logPath, logLines);
        LogHtml.generateReport(logPath.toFile(), htmlPath.toFile());

        assertTrue(Files.exists(htmlPath), "HTML report should be generated");
        final String html = Files.readString(htmlPath);

        // When > 50 tests, line graph is the default active view
        assertTrue(html.contains("id=\"timelineLineContainer\" class=\"timeline-view-line\""), "Line graph container should be active (not hidden)");
        assertTrue(html.contains("id=\"timelineBarsContainer\" class=\"chart-scroll-wrap timeline-view-bars hidden\""), "Bars container should be hidden by default");
        assertTrue(html.contains("id=\"btnViewLine\" class=\"chart-toggle-btn active\""), "Line toggle button should be active");
        assertTrue(html.contains("class=\"timeline-line-svg\""), "SVG line graph should be rendered");
        assertTrue(html.contains("window.lgData = ["), "Interactive line graph script data should be embedded");
        assertTrue(html.contains("Test Time (s)"), "Legend should contain Test Time");
    }

    @Test
    void rendersLeafCompletionGraphFromSeparateDataSeries(@TempDir final Path tempDir) throws IOException {
        final Path logPath = tempDir.resolve("all_leafs.run.log");
        final Path htmlPath = tempDir.resolve("all_leafs.run.log.html");
        final Path completionPath = tempDir.resolve("all_leafs.all-leaf-completions.log");
        Files.write(logPath, List.of(
                "[ 1][1][0.000][10] Executing First Scenario",
                "[ 1][2][10.000][5] • Closing test 1 ...",
                "[  ][3][10.010][2] • Test time 10 s",
                "[ 2][4][11.000][10] Executing Second Scenario",
                "[ 2][5][12.000][5] • Closing test 2 ...",
                "[  ][6][12.010][2] • Test time 1 s",
                "[ 3][7][13.000][10] Executing Third Scenario",
                "[ 3][8][14.000][5] • Closing test 3 ...",
                "[  ][9][14.010][2] • Test time 1 s"));
        Files.write(completionPath, List.of(
                "[ALL_LEAF_COMPLETION] 1 1000 checkout.Cart",
                "[ALL_LEAF_COMPLETION] 2 4000 checkout.Payment",
                "[ALL_LEAF_COMPLETION] 3 4500 checkout.Confirmation"));

        LogHtml.generateReport(logPath.toFile(), htmlPath.toFile());

        final String html = Files.readString(htmlPath);
        assertTrue(html.contains("All-Leaf Completion Intervals"));
        assertTrue(html.contains("checkout.Cart @ 0:01.000 (+1.00s)"));
        assertTrue(html.contains("checkout.Payment @ 0:04.000 (+3.00s)"));
        assertTrue(html.contains("checkout.Confirmation @ 0:04.500 (+500ms)"));
        assertTrue(html.contains("Projected Total (Passed + ETA)"));
        assertTrue(html.indexOf("All-Leaf Completion Intervals") < html.indexOf("Test Executions &amp; Limbo Durations"));
        assertTrue(html.contains("<section id=\"leafCompletionChart\""));
        assertTrue(html.contains("<svg id=\"leafCompletionSvg\" class=\"timeline-line-svg\""));
        assertFalse(html.contains("leaf-chart-scroll-wrap"), "Leaf completion chart should never render a horizontal scroll wrapper");
        assertTrue(html.contains("viewBox=\"0 0 1000 240\""),
                "Leaf completion SVG width should be controlled by responsive CSS");
        assertTrue(html.contains("<path d=\"M 55.00 136.37 L 500.00 66.73 L 945.00 165.27\" class=\"leaf-chart-line\""),
                "Leaf completion points should be distributed evenly across the chart");
        assertTrue(html.contains("window.lgData = [{\"n\":1"));
        assertTrue(html.contains("\"s\":10.000"));
        assertTrue(html.contains("\"s\":1.000"));
        final int leafChartStart = html.indexOf("leafCompletionChart");
        final int leafChartEnd = html.indexOf("</section>", leafChartStart);
        assertFalse(html.substring(leafChartStart, leafChartEnd)
                .contains("window.lgData"), "Leaf chart must not use the test/limbo timeline data");
    }

    @Test
    void omitsLeafCompletionGraphWithoutAllLeafCompletionMarkers(@TempDir final Path tempDir) throws IOException {
        final Path logPath = tempDir.resolve("standard.run.log");
        final Path htmlPath = tempDir.resolve("standard.run.log.html");
        Files.write(logPath, List.of("[ 1][1][1.000][10] [ALL_LEAF_COMPLETION] 1 1000 legacy.MainLogMarker"));

        LogHtml.generateReport(logPath.toFile(), htmlPath.toFile());

        assertFalse(Files.readString(htmlPath).contains("All-Leaf Completion Intervals"));
    }

    @Test
    void rendersLeafCompletionGraphWithEtaSeries(@TempDir final Path tempDir) throws IOException {
        final Path logPath = tempDir.resolve("eta_test.run.log");
        final Path htmlPath = tempDir.resolve("eta_test.run.log.html");
        final Path completionPath = tempDir.resolve("eta_test.all-leaf-completions.log");
        Files.write(logPath, List.of(
                "[ 1][1][0.000][10] Executing Scenario",
                "[ 1][2][10.000][5] • Closing test 1 ...",
                "[  ][3][10.010][2] • Test time 10 s",
                "[ 2][4][11.000][10] Executing Second Scenario",
                "[ 2][5][12.000][5] • Closing test 2 ...",
                "[  ][6][12.010][2] • Test time 1 s"));
        Files.write(completionPath, List.of(
                "[ALL_LEAF_COMPLETION] 1 1000 20000 checkout.Cart",
                "[ALL_LEAF_COMPLETION] 2 4000 15000 checkout.Payment"));

        LogHtml.generateReport(logPath.toFile(), htmlPath.toFile());

        final String html = Files.readString(htmlPath);
        assertTrue(html.contains("All-Leaf Completion Intervals"));
        assertTrue(html.contains("checkout.Cart @ 0:01.000 (+1.00s, Projected: 21.00s)"));
        assertTrue(html.contains("checkout.Payment @ 0:04.000 (+3.00s, Projected: 19.00s)"));
        assertTrue(html.contains("class=\"leaf-eta-chart-line\""));
        assertTrue(html.contains("leaf-eta-line"));
        assertTrue(html.contains("window.leafCeil = "));
        assertTrue(html.contains("window.leafCeilInterval = "));
        assertTrue(html.contains("window.leafCeilProjected = "));
    }

    @Test
    void rendersEtaDebugGraphWithCorrectCeilings(@TempDir final Path tempDir) throws IOException {
        final Path logPath = tempDir.resolve("eta_debug.run.log");
        final Path htmlPath = tempDir.resolve("eta_debug.run.log.html");
        final Path etaDebugPath = tempDir.resolve("eta_debug.eta-debug.log");
        Files.write(logPath, List.of(
                "[ 1][1][0.000][10] Executing Scenario",
                "[ 1][2][10.000][5] • Closing test 1 ...",
                "[  ][3][10.010][2] • Test time 10 s",
                "[ 2][4][11.000][10] Executing Second Scenario",
                "[ 2][5][12.000][5] • Closing test 2 ...",
                "[  ][6][12.010][2] • Test time 1 s"));
        Files.write(etaDebugPath, List.of(
                "[ETA_DEBUG] 1 1000 5000 1000 5500 TestScenario1",
                "[ETA_DEBUG] 2 2000 5000 1000 5200 TestScenario2"));

        LogHtml.generateReport(logPath.toFile(), htmlPath.toFile());

        final String html = Files.readString(htmlPath);
        assertTrue(html.contains("ETA Debug"));
        assertTrue(html.contains("window.etaDebugCeil = "));
        assertTrue(html.contains("window.etaDebugCeilInstability = "));
        assertTrue(html.contains("window.etaDebugCeilError = "));
    }

    @Test
    void rendersSystemResourcesGraphWithTelemetryData(@TempDir final Path tempDir) throws IOException {
        final Path logPath = tempDir.resolve("res_test.run.log");
        final Path htmlPath = tempDir.resolve("res_test.run.log.html");
        final Path telemPath = tempDir.resolve("res_test.telemetry.log");
        Files.write(logPath, List.of(
                "[ 1][1][0.000][10] Executing Scenario",
                "[ 1][2][10.000][5] • Closing test 1 ...",
                "[  ][3][10.010][2] • Test time 10 s",
                "[ 2][4][11.000][10] Executing Second Scenario",
                "[ 2][5][12.000][5] • Closing test 2 ...",
                "[  ][6][12.010][2] • Test time 1 s"));
        Files.write(telemPath, List.of(
                "0,100,12.5,100,4,8,10,0,10,104857600,524288000,1048576000,sample1",
                "0,200,25.0,200,4,8,10,0,10,209715200,524288000,1048576000,sample2"));

        LogHtml.generateReport(logPath.toFile(), htmlPath.toFile());

        final String html = Files.readString(htmlPath);
        assertTrue(html.contains("System Resources (CPU &amp; RAM)"));
        assertTrue(html.contains("window.resourceData = ["));
        assertTrue(html.contains("\"cpu\":12.50"));
        assertTrue(html.contains("\"ramUsed\":400.0"));
    }

    @Test
    void testTelemetryDownsamplingWithPeakTracking() {
        final List<works.lysenko.base.output.loghtml.LogModels.SystemResourceItem> raw = new ArrayList<>();
        for (int i = 1; i <= 100; i++) {
            final double cpu = (i % 10 == 0) ? 90.0 : 10.0;
            raw.add(new works.lysenko.base.output.loghtml.LogModels.SystemResourceItem(i, cpu, 100.0, 500.0, 4));
        }

        final List<works.lysenko.base.output.loghtml.LogModels.SystemResourceItem> downsampled =
                works.lysenko.base.output.loghtml.SidecarLoader.downsampleTelemetry(raw, 10);

        assertEquals(10, downsampled.size());
        for (final works.lysenko.base.output.loghtml.LogModels.SystemResourceItem item : downsampled) {
            assertEquals(18.0, item.cpuPct, 1e-2); // Average CPU (9 items of 10.0 + 1 item of 90.0 = 180 / 10 = 18.0)
            assertEquals(90.0, item.maxCpuPct, 1e-2); // Peak CPU tracked in bucket
        }
    }

    @Test
    void testTelemetryDensityPerTest(@TempDir final Path tempDir) throws IOException {
        final Path logPath = tempDir.resolve("density_test.run.log");
        final Path telemPath = tempDir.resolve("density_test.telemetry.log");
        final List<String> telemLines = new ArrayList<>();
        for (int i = 1; i <= 300; i++) {
            telemLines.add("0," + i + ",20.0,100,4,8,10,0,10,104857600,524288000,1048576000,sample" + i);
        }
        Files.write(telemPath, telemLines);
        Files.write(logPath, List.of(
                "[ 1][1][0.000][10] Executing Scenario",
                "[ 1][2][10.000][5] • Closing test 1 ...",
                "[ 2][3][11.000][10] Executing Scenario",
                "[ 2][4][20.000][5] • Closing test 2 ...",
                "[ 3][5][21.000][10] Executing Scenario",
                "[ 3][6][30.000][5] • Closing test 3 ..."));

        final works.lysenko.base.TestProperties previousProps = works.lysenko.Base.properties;
        try {
            final works.lysenko.base.TestProperties testProps = new works.lysenko.base.TestProperties();
            final java.lang.reflect.Field f = works.lysenko.base.TestProperties.class.getDeclaredField("the");
            f.setAccessible(true);
            final java.util.Properties props = new java.util.Properties();
            props.setProperty(".test.report.cpu.density.per.test", "true");
            f.set(testProps, props);
            works.lysenko.Base.properties = testProps;

            final List<works.lysenko.base.output.loghtml.LogModels.SystemResourceItem> items =
                    works.lysenko.base.output.loghtml.SidecarLoader.loadTelemetryResources(logPath.toFile(), 3);

            assertEquals(3, items.size());
        } catch (final Exception e) {
            throw new RuntimeException(e);
        } finally {
            works.lysenko.Base.properties = previousProps;
        }
    }

    @Test
    void testTelemetryDensityPerTestWithoutLeadingDot(@TempDir final Path tempDir) throws IOException {
        final Path logPath = tempDir.resolve("density_test_nodot.run.log");
        final Path telemPath = tempDir.resolve("density_test_nodot.telemetry.log");
        final List<String> telemLines = new ArrayList<>();
        for (int i = 1; i <= 500; i++) {
            telemLines.add("0," + i + ",20.0,100,4,8,10,0,10,104857600,524288000,1048576000,sample" + i);
        }
        Files.write(telemPath, telemLines);

        final works.lysenko.base.TestProperties previousProps = works.lysenko.Base.properties;
        try {
            final works.lysenko.base.TestProperties testProps = new works.lysenko.base.TestProperties();
            final java.lang.reflect.Field f = works.lysenko.base.TestProperties.class.getDeclaredField("the");
            f.setAccessible(true);
            final java.util.Properties props = new java.util.Properties();
            props.setProperty("test.report.cpu.density.per.test", "true");
            f.set(testProps, props);
            works.lysenko.Base.properties = testProps;

            final List<works.lysenko.base.output.loghtml.LogModels.SystemResourceItem> items =
                    works.lysenko.base.output.loghtml.SidecarLoader.loadTelemetryResources(logPath.toFile(), 37);

            assertEquals(37, items.size());
        } catch (final Exception e) {
            throw new RuntimeException(e);
        } finally {
            works.lysenko.Base.properties = previousProps;
        }
    }

    @Test
    void testSystemResourcesTrimmingPreAndPostTestSamples(@TempDir final Path tempDir) throws IOException {
        final Path logPath = tempDir.resolve("trim_test.run.log");
        final Path telemPath = tempDir.resolve("trim_test.telemetry.log");

        // 2 pre-test samples (1, 2), 4 test/limbo samples (3, 4, 5, 6), 2 post-test samples (7, 8)
        final List<String> logLines = List.of(
                "Starting Bot core ...",
                "# Applied test configuration",
                "[ ][1][.010][9] Initializing Driver ...",                     // pre-test op 1
                "[ ][2][.020][1] Preflight checks ...",                       // pre-test op 2
                "[ 1][3][1.000][10] Executing Scenario A",                    // test 1 start op 3
                "[ 1][4][1.050][5] • Closing test 1 ...",                     // test 1 end op 4
                "[ ][5][1.060][2] • Test time 50 ms",                         // limbo op 5
                "[ 2][6][2.000][10] Executing Scenario B",                    // test 2 end op 6
                "[ ][7][2.070][1] 2 tests of Sample done in 2 s",              // postflight op 7
                "[ ][8][2.080][1] = Event summary ="                           // postflight op 8
        );

        final List<String> telemLines = List.of(
                "0,1,99.0,100,4,8,10,0,10,104857600,524288000,1048576000,pre_1",
                "0,2,95.0,100,4,8,10,0,10,104857600,524288000,1048576000,pre_2",
                "0,3,10.0,100,4,8,10,0,10,104857600,524288000,1048576000,test_1",
                "0,4,12.0,100,4,8,10,0,10,104857600,524288000,1048576000,test_2",
                "0,5,15.0,100,4,8,10,0,10,104857600,524288000,1048576000,test_3",
                "0,6,11.0,100,4,8,10,0,10,104857600,524288000,1048576000,test_4",
                "0,7,88.0,100,4,8,10,0,10,104857600,524288000,1048576000,post_1",
                "0,8,90.0,100,4,8,10,0,10,104857600,524288000,1048576000,post_2"
        );

        Files.write(logPath, logLines);
        Files.write(telemPath, telemLines);

        final int[] bounds = works.lysenko.base.output.loghtml.SidecarLoader.findTestOpBounds(logPath.toFile());
        assertEquals(3, bounds[0], "First test operation should be op 3");
        assertEquals(6, bounds[1], "Last test operation should be op 6");

        final List<works.lysenko.base.output.loghtml.LogModels.SystemResourceItem> items =
                works.lysenko.base.output.loghtml.SidecarLoader.loadTelemetryResources(logPath.toFile(), 0);

        assertEquals(4, items.size(), "Should only retain 4 samples from test period (ops 3..6)");
        assertEquals(3, items.get(0).sampleNum);
        assertEquals(10.0, items.get(0).cpuPct, 1e-2);
        assertEquals(6, items.get(3).sampleNum);
        assertEquals(11.0, items.get(3).cpuPct, 1e-2);
    }
}
