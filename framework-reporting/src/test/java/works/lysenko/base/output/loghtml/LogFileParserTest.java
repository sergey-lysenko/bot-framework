package works.lysenko.base.output.loghtml;

import org.junit.jupiter.api.Test;
import works.lysenko.base.output.loghtml.parser.LogFileParser;

import java.io.BufferedReader;
import java.io.File;
import java.io.StringReader;

import static org.junit.jupiter.api.Assertions.*;

class LogFileParserTest {

    @Test
    void testParseScenarioStatistics() throws Exception {
        final String logContent = """
                [  ][2607][176.524][    1] = 24 Scenarios statistics =
                [  ][2608][176.525][    1] 48 paths were possible with current set of Scenarios
                [  ][2609][176.526][    0] 17 (35.4%) among these had a chance to be executed
                [  ][2610][176.526][    1] 0 (0.0%) among these were actually executed
                [  ][2611][176.527][    0] ● biteheist.util.test.Specifics........................................... [0] 7 (7 events)
                [  ][2612][176.527][    1] ▷ SignIn.................................................................. [0] ← (2) 25
                [  ][2613][176.528][    0] ▷ SignIn.CorrectLogin..................................................... [0] ← (2) 25
                [  ][2614][176.528][    1] ▷ SignIn.CorrectLogin.Partners............................................ [1] 10
                [  ][2615][176.529][    0] ▷ SignIn.CorrectLogin.Partners.AddNew..................................... (1) → [0] 10
                [  ][2616][176.529][    1] ◼ SignIn.CorrectLogin.Partners.AddNew.AllEmpty............................ (1) → [0] 1 (6 events)
                """;

        final LogModels.LineStore lineStore = new LogModels.LineStore();

        try (final BufferedReader reader = new BufferedReader(new StringReader(logContent))) {
            final LogFileParser.ParseResult result = LogFileParser.parse(reader, new File("dummy.log"), lineStore, null);

            assertEquals("48 paths were possible with current set of Scenarios", result.pathsPossibleStr);
            assertEquals("17 (35.4%) among these had a chance to be executed", result.pathsChanceStr);
            assertEquals("0 (0.0%) among these were actually executed", result.pathsExecutedStr);

            assertEquals(6, result.scenStats.size(), "Should parse 6 scenario statistics rows");

            final LogModels.ScenEntry first = result.scenStats.get(0);
            assertEquals("●", first.sym);
            assertEquals("biteheist.util.test.Specifics", first.name);
            assertEquals("[0]", first.weight);
            assertEquals("7", first.count);
            assertEquals("7 events", first.events);

            final LogModels.ScenEntry second = result.scenStats.get(1);
            assertEquals("▷", second.sym);
            assertEquals("SignIn", second.name);
            assertEquals("[0] ← (2)", second.weight);
            assertEquals("25", second.count);
            assertEquals("", second.events);
        }
    }

    @Test
    void testParseConfiguringSectionWithParametersBlock() throws Exception {
        final String logContent = """
                Boot banner line
                Reading Common Configuration properties ...
                {common={directory=config/}, {name=common.configuration}, {extension=.properties}}
                 ... spent 4 ms 
                Reading Parameters ...
                {parameters={file=var/parameters}, {mode=CLI}, {env=1}}
                # Applied execution parameters
                •SUITE=smoke     *TESTS=10
                --------------------------
                # Unchanged execution parameters with default values
                PLATFORM = chrome     HEADLESS = false
                Values of 2 execution parameters were redefined, 16 have default values
                 ... spent 8 ms 
                Reading smoke Test Properties ...
                # Applied test configuration
                .test.tests=10
                [  ][   1][  0.100][    5] Preflight step
                """;

        final LogModels.LineStore lineStore = new LogModels.LineStore();
        try (final BufferedReader reader = new BufferedReader(new StringReader(logContent))) {
            final LogFileParser.ParseResult result = LogFileParser.parse(reader, new File("dummy.log"), lineStore, null);
            assertEquals(3, result.sections.size(), "Should produce Booting, Configuring, and Preflight sections");
            assertEquals("booting", result.sections.get(0).type);
            assertEquals("configuring", result.sections.get(1).type);
            assertEquals("preflight", result.sections.get(2).type);

            final String configJoined = String.join("\n", result.sections.get(1).lines);
            assertTrue(configJoined.contains("Reading Parameters ..."));
            assertTrue(configJoined.contains("# Applied execution parameters"));
            assertTrue(configJoined.contains("Values of 2 execution parameters were redefined"));
        }
    }
}
