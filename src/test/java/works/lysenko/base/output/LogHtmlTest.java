package works.lysenko.base.output;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class LogHtmlTest {

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
                "[ ][15][2.101][0] 37 (100.0%) among these had a chance to be executed"
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

        // Verify scenario statistics subtitle contains both lines
        assertTrue(html.contains("37 paths were possible with current set of Scenarios"), "Should include paths possible string in HTML");
        assertTrue(html.contains("37 (100.0%) among these had a chance to be executed"), "Should include paths chance string in HTML");
    }
}
