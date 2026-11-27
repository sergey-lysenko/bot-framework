package works.lysenko.base.output.loghtml;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import works.lysenko.base.issues.KnownIssuesStore;
import works.lysenko.base.output.SummaryPlaque;
import works.lysenko.base.output.TreeHtml;
import works.lysenko.base.output.loghtml.LogModels.LineStore;
import works.lysenko.base.output.loghtml.LogModels.LogSection;
import works.lysenko.base.output.loghtml.parser.LogFileParser;
import works.lysenko.base.output.plaque.PlaqueMetricsCalculator;
import works.lysenko.base.responses.ExpectedResponsesStore;

import java.io.BufferedReader;
import java.io.File;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LogParserRegressionTest {

    private KnownIssuesStore previousKnownStore;
    private KnownIssuesStore previousFixedStore;
    private ExpectedResponsesStore previousExpectedStore;

    @BeforeEach
    void setUp() {
        previousKnownStore = TreeHtml.getKnownIssuesStore();
        previousFixedStore = TreeHtml.getFixedIssuesStore();
        previousExpectedStore = TreeHtml.getExpectedResponsesStore();

        TreeHtml.setKnownIssuesStore(new KnownIssuesStore(List.of()));
        TreeHtml.setFixedIssuesStore(new KnownIssuesStore(List.of()));
        TreeHtml.setExpectedResponsesStore(new ExpectedResponsesStore(List.of()));
    }

    @AfterEach
    void tearDown() {
        TreeHtml.setKnownIssuesStore(previousKnownStore);
        TreeHtml.setFixedIssuesStore(previousFixedStore);
        TreeHtml.setExpectedResponsesStore(previousExpectedStore);
    }

    @Test
    @DisplayName("Parses sidecar fixed.issues.json and applies severity-wins / regression-wins-ties rules across sections and HTML")
    void testLogSidecarFixedIssuesAndPrecedence(@TempDir final Path tempDir) throws Exception {
        final File logFile = tempDir.resolve("1700000000000.run.log").toFile();
        final File fixedSidecar = tempDir.resolve("1700000000000.fixed.issues.json").toFile();
        final File knownSidecar = tempDir.resolve("1700000000000.known.issues.json").toFile();

        Files.writeString(fixedSidecar.toPath(), """
                {
                  "$schema": "./issues.schema.json",
                  "issues": [
                    {
                      "scenario": "Checkout.Pay",
                      "title": "FIX-1",
                      "description": "Fixed checkout crash",
                      "link": "https://jira.example.com/FIX-1",
                      "pattern": "OLD_CHECKOUT_CRASH",
                      "resolvedAt": "2026-04-10T10:00:00Z"
                    },
                    {
                      "scenario": "Profile.Edit",
                      "title": "FIX-2",
                      "description": "Fixed profile warning",
                      "link": "https://jira.example.com/FIX-2",
                      "pattern": "OLD_PROFILE_WARN"
                    },
                    {
                      "scenario": "Mixed.Severity",
                      "title": "FIX-3",
                      "description": "Fixed minor warning",
                      "link": "https://jira.example.com/FIX-3",
                      "pattern": "OLD_MINOR_WARN"
                    },
                    {
                      "scenario": "Reopened.Flow",
                      "title": "FIX-4",
                      "description": "Previously fixed but now active known",
                      "link": "https://jira.example.com/FIX-4",
                      "pattern": "REOPENED_PATTERN"
                    }
                  ]
                }
                """, StandardCharsets.UTF_8);

        Files.writeString(knownSidecar.toPath(), """
                {
                  "$schema": "./issues.schema.json",
                  "issues": [
                    {
                      "scenario": "Reopened.Flow",
                      "title": "ACT-4",
                      "description": "Active known issue takes precedence",
                      "link": "https://jira.example.com/ACT-4",
                      "pattern": "REOPENED_PATTERN",
                      "decrease-to-warning": true
                    }
                  ]
                }
                """, StandardCharsets.UTF_8);

        final String logContent = """
                [  ][   1][  0.100][    5] Preflight step
                [ 1][   2][  1.000][  100] ◼ Checkout.Pay: starting
                [ 1][   3][  1.100][  100] [FAILURE] Brand new S1 failure
                [ 1][   4][  1.200][  100] [FAILURE] OLD_CHECKOUT_CRASH returned
                [ 2][   5][  2.000][  100] ◼ Profile.Edit: starting
                [ 2][   6][  2.100][  100] [WARNING] Unknown warning
                [ 2][   7][  2.200][  100] [WARNING] OLD_PROFILE_WARN returned
                [ 3][   8][  3.000][  100] ◼ Mixed.Severity: starting
                [ 3][   9][  3.100][  100] [WARNING] OLD_MINOR_WARN returned
                [ 3][  10][  3.200][  100] [FAILURE] Brand new unknown S1 error
                [ 4][  11][  4.000][  100] ◼ Reopened.Flow: starting
                [ 4][  12][  4.100][  100] [FAILURE] REOPENED_PATTERN occurred
                [ 5][  13][  5.000][  100] ◼ Clean.Flow: starting
                [  ][  14][  6.000][   10] = [FAILURE] Test run finished with failures =
                """;
        Files.writeString(logFile.toPath(), logContent, StandardCharsets.UTF_8);

        final LineStore lineStore = new LineStore();
        final LogFileParser.ParseResult parsed;
        try (final BufferedReader reader = new BufferedReader(new StringReader(logContent))) {
            parsed = LogFileParser.parse(reader, logFile, lineStore, null);
        }

        final List<LogSection> testSections = parsed.sections.stream()
                .filter(s -> "test".equals(s.type))
                .toList();
        assertEquals(5, testSections.size());

        // Test 1: tie between unknown FAILURE and fixed FAILURE -> Regression wins!
        assertEquals("regression", testSections.get(0).status);
        // Test 2: tie between unknown WARNING and fixed WARNING -> Regression Warning wins!
        assertEquals("regression_warning", testSections.get(1).status);
        // Test 3: unknown FAILURE + fixed WARNING -> Severity wins (red failed)!
        assertEquals("failed", testSections.get(2).status);
        // Test 4: active known issue (decrease-to-warning: true) beats fixed issue -> warning!
        assertEquals("warning", testSections.get(3).status);
        // Test 5: clean pass
        assertEquals("passed", testSections.get(4).status);

        assertEquals("regression", SectionRenderer.computeScenarioStatuses(testSections.get(0)).get("Checkout.Pay"));
        assertEquals("regression_warning", SectionRenderer.computeScenarioStatuses(testSections.get(1)).get("Profile.Edit"));
        assertEquals("failed", SectionRenderer.computeScenarioStatuses(testSections.get(2)).get("Mixed.Severity"));
        assertEquals("warning", SectionRenderer.computeScenarioStatuses(testSections.get(3)).get("Reopened.Flow"));
        assertEquals("passed", SectionRenderer.computeScenarioStatuses(testSections.get(4)).get("Clean.Flow"));

        final SummaryPlaque plaque = PlaqueMetricsCalculator.computeFromSections(parsed.sections);
        assertEquals(1, plaque.regression());
        assertEquals(1, plaque.regressionWarning());
        assertEquals(1, plaque.failed());
        assertEquals(1, plaque.warning());
        assertEquals(1, plaque.passed());

        final LineStore htmlStore = new LineStore();
        final String html;
        try (final BufferedReader reader = new BufferedReader(new StringReader(logContent))) {
            html = LogParser.buildHtml(reader, logFile, false, false, false, false, htmlStore);
        }
        assertTrue(html.contains("class=\"log-section test regression\""), "HTML must render vivid-magenta failure regression section");
        assertTrue(html.contains("class=\"log-section test regression_warning\""), "HTML must render soft-magenta warning regression section");
        assertTrue(html.contains("class=\"result-plaque regression\""), "Overall result plaque must upgrade to regression");
        assertTrue(html.contains("class=\"badge regression\">regression<"), "Regression badge must appear in rendered HTML");
        assertTrue(html.contains("class=\"badge regression_warning\">reg. warn"), "Regression warning badge must appear in rendered HTML");
        assertTrue(html.contains("class=\"pill scen-pill regression\""), "Scenario pill must render vivid-magenta regression class");
        assertTrue(html.contains("class=\"pill scen-pill regression_warning\""), "Scenario pill must render soft-magenta regression_warning class");
        assertFalse(html.contains("{{"));
    }

    @Test
    @DisplayName("SectionRenderer.promoteStatus enforces Severity wins; Regression wins ties")
    void testSectionRendererPromoteStatus() {
        assertEquals("regression", SectionRenderer.promoteStatus("failed", "regression"));
        assertEquals("regression", SectionRenderer.promoteStatus("regression", "failed"));
        assertEquals("failed", SectionRenderer.promoteStatus("failed", "regression_warning"));
        assertEquals("failed", SectionRenderer.promoteStatus("regression_warning", "failed"));
        assertEquals("regression_warning", SectionRenderer.promoteStatus("warning", "regression_warning"));
        assertEquals("regression_warning", SectionRenderer.promoteStatus("regression_warning", "warning"));
        assertEquals("warning", SectionRenderer.promoteStatus("notice", "warning"));
        assertEquals("notice", SectionRenderer.promoteStatus("passed", "notice"));
    }
}
