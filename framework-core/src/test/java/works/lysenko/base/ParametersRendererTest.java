package works.lysenko.base;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import works.lysenko.Base;
import works.lysenko.util.data.enums.Ansi;
import works.lysenko.util.data.enums.ExecutionParameter;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies the startup execution parameters configuration block output and diagnostics.
 */
class ParametersRendererTest {

    private Parameters previousParameters;
    private PrintStream originalOut;
    private ByteArrayOutputStream capturedOut;

    @BeforeEach
    void setUp() {

        previousParameters = Base.parameters;
        originalOut = System.out;
        capturedOut = new ByteArrayOutputStream();
        System.setOut(new PrintStream(capturedOut, true, StandardCharsets.UTF_8));
    }

    @AfterEach
    void tearDown() {

        System.setOut(originalOut);
        Base.parameters = previousParameters;
    }

    private String cleanOutput() {

        return Ansi.removeANSICodes(capturedOut.toString(StandardCharsets.UTF_8));
    }

    private String rawOutput() {

        return capturedOut.toString(StandardCharsets.UTF_8);
    }

    @Test
    void rendersAllDefaultsWhenNoParametersAreRedefined() {

        final Parameters parameters = new Parameters(new Properties());
        parameters.logParametersConfiguration();

        final String clean = cleanOutput();
        final int totalStandard = ExecutionParameter.values().length;

        assertTrue(clean.contains("{parameters={file=none}, {mode=CLI}, {env=0}}"), "Should render gray debug summary");
        assertTrue(clean.contains("# Applied execution parameters"), "Should render applied section header");
        assertTrue(clean.contains("(none)"), "Should render (none) when no parameters are redefined");
        assertTrue(clean.contains("# Unchanged execution parameters with default values"), "Should render defaults section header");
        assertTrue(clean.contains("PLATFORM = Chrome"), "Should list PLATFORM default");
        assertTrue(clean.contains("EXHAUSTIVE_PASSES = 1"), "Should list EXHAUSTIVE_PASSES default");
        assertTrue(clean.contains("Values of 0 execution parameters were redefined, " + totalStandard + " have default values"),
                "Summary line should match exact counts, actual:\n" + clean);
    }

    @Test
    void rendersFileEnvAndSessionParametersWithMarkersAndColors() {

        final Properties props = new Properties();
        props.setProperty("SUITE", "smoke_suite");
        props.setProperty("OLD_SUITE", "smoke_suite");
        props.setProperty("TESTS", "25");
        props.setProperty("DOMAIN", "staging.example.com");
        props.setProperty("HEADLESS", "false");

        final Parameters parameters = new Parameters(props);
        parameters.recordFileValue("SUITE", "smoke_suite");
        parameters.recordFileValue("OLD_SUITE", "smoke_suite");
        parameters.recordEnvValue("TESTS", "25");

        parameters.logParametersConfiguration();

        final String raw = rawOutput();
        final String clean = cleanOutput();
        final int expectedDefaults = ExecutionParameter.values().length - 3;

        assertTrue(clean.contains("{parameters={file=var/parameters}, {mode=CLI}, {env=1}}"), "Debug summary should show file and env count");
        assertTrue(clean.contains("•SUITE=smoke_suite"), "File-sourced parameter should carry bullet marker");
        assertTrue(clean.contains("*TESTS=25"), "Environment-sourced parameter should carry asterisk marker");
        assertTrue(clean.contains("DOMAIN=staging.example.com"), "Session-modified parameter should appear without file/env marker");
        assertFalse(clean.contains("•DOMAIN="), "Session-modified parameter must not have file marker");
        assertFalse(clean.contains("*DOMAIN="), "Session-modified parameter must not have env marker");
        assertTrue(raw.contains(Ansi.mb("TESTS")), "Environment override key should be magenta bold");
        assertTrue(clean.contains("OLD_SUITE = smoke_suite"), "Matching OLD_SUITE should stay in unchanged defaults");
        assertTrue(clean.contains("TAG = smoke_suite"), "Computed TAG default should match SUITE in unchanged defaults");
        assertTrue(clean.contains("Values of 3 execution parameters were redefined, " + expectedDefaults + " have default values"),
                "Summary counts should reflect 3 redefined parameters, actual:\n" + clean);
    }

    @Test
    void masksPasswordAdditionalParametersAndReportsAdditionalAndUnknownCounts() {

        final Properties props = new Properties();
        props.setProperty("API_USER", "alice");
        props.setProperty("API_SECRET", "s3cr3t-p@ssw0rd");
        props.setProperty("EXTRA_EMPTY", "");
        props.setProperty("STALE_LEGACY_PARAM", "obsolete");

        final Parameters parameters = new Parameters(props);
        parameters.setAdditionalParameters(
                new String[]{"API_USER", "API_SECRET", "EXTRA_EMPTY"},
                new String[]{"t", "p", "t"}
        );

        parameters.logParametersConfiguration();

        final String raw = rawOutput();
        final String clean = cleanOutput();

        assertFalse(raw.contains("s3cr3t-p@ssw0rd"), "Password parameter value must never be printed in cleartext");
        assertTrue(clean.contains("API_USER=alice"), "Text additional parameter should display value");
        assertTrue(clean.contains("API_SECRET=********"), "Password additional parameter should be masked");
        assertTrue(raw.contains(Ansi.gb("API_USER")), "Additional parameter key should be green bold");
        assertTrue(raw.contains(Ansi.rb("STALE_LEGACY_PARAM=obsolete")), "Unknown parameter should be red bold");
        assertTrue(clean.contains("EXTRA_EMPTY ="), "Unset additional parameter should appear in unchanged defaults");
        assertTrue(clean.contains("There are 3 additional execution parameters"), "Should report additional parameter count");
        assertTrue(clean.contains("one unknown execution parameter"), "Should report unknown parameter count, actual:\n" + clean);
    }
}
