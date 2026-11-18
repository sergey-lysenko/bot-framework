package works.lysenko.base;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import works.lysenko.Base;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Precedence rules of execution parameters (regression guard for ".all.leafs.executions" being ignored).
 */
class ParametersTest {

    private Parameters previousParameters;
    private TestProperties previousProperties;

    @BeforeEach
    void setUp() {

        previousParameters = Base.parameters;
        previousProperties = Base.properties;
        Base.properties = null;
    }

    @AfterEach
    void tearDown() {

        Base.parameters = previousParameters;
        Base.properties = previousProperties;
    }

    private static Parameters with(final String... keyValues) {

        final Properties p = new Properties();
        for (int i = 0; i < keyValues.length; i += 2) p.put(keyValues[i], keyValues[i + 1]);
        return new Parameters(p);
    }

    @Test
    void defaultsWhenNothingIsSet() {

        final Parameters parameters = with();
        assertFalse(parameters.isAllLeafs());
        assertEquals(1, parameters.getAllLeafsExecutions());
        assertFalse(parameters.isHeadless());
    }

    @Test
    void countAboveOneImpliesAllLeafs() {

        final Parameters parameters = with("ALL_LEAFS_EXECUTIONS", "3");
        assertEquals(3, parameters.getAllLeafsExecutions());
        assertTrue(parameters.isAllLeafs());
    }

    @Test
    void countAboveOneOverridesExplicitFalse() {

        final Parameters parameters = with("ALL_LEAFS", "false", "ALL_LEAFS_EXECUTIONS", "3");
        assertTrue(parameters.isAllLeafs());
        assertEquals(3, parameters.getAllLeafsExecutions());
    }

    @Test
    void explicitAllLeafsWithSinglePass() {

        final Parameters parameters = with("ALL_LEAFS", "true", "ALL_LEAFS_EXECUTIONS", "1");
        assertTrue(parameters.isAllLeafs());
        assertEquals(1, parameters.getAllLeafsExecutions());
    }

    @Test
    void explicitFalseWithSinglePass() {

        final Parameters parameters = with("ALL_LEAFS", "false", "ALL_LEAFS_EXECUTIONS", "1");
        assertFalse(parameters.isAllLeafs());
    }

    @Test
    void emptyValuesFallBackToDefaults() {

        final Parameters parameters = with("ALL_LEAFS", "", "ALL_LEAFS_EXECUTIONS", "");
        assertFalse(parameters.isAllLeafs());
        assertEquals(1, parameters.getAllLeafsExecutions());
    }

    @Test
    void countIsClampedToAtLeastOne() {

        assertEquals(1, with("ALL_LEAFS_EXECUTIONS", "0").getAllLeafsExecutions());
        assertEquals(1, with("ALL_LEAFS_EXECUTIONS", "-5").getAllLeafsExecutions());
    }

    @Test
    void malformedCountIsIgnored() {

        final Parameters parameters = with("ALL_LEAFS_EXECUTIONS", "many");
        assertEquals(1, parameters.getAllLeafsExecutions());
        assertFalse(parameters.isAllLeafs());
    }

    @Test
    void headlessFollowsExplicitValue() {

        assertTrue(with("HEADLESS", "true").isHeadless());
        assertFalse(with("HEADLESS", "false").isHeadless());
    }

    @Test
    void plainAccessors() {

        final Parameters parameters = with("TEST", "smoke", "POOL", "fast", "DOMAIN", "dev", "TESTS", "10", "FORBID_OVEREXECUTION", "true", "COMPLETION_WEIGHT", "2.5", "TRAVERSE_EXTENSIONS", "true");
        assertEquals("smoke", parameters.getTest());
        assertEquals("fast", parameters.getPool());
        assertEquals("dev", parameters.getDomain());
        assertEquals("10", parameters.getTests());
        assertEquals(Boolean.TRUE, parameters.getForbidOverexecution());
        assertEquals("2.5", parameters.getCompletionWeight());
        assertEquals(Boolean.TRUE, parameters.getTraverseExtensions());
    }

    @Test
    void testBackdraftOnSuiteChange() throws Exception {
        final java.io.File suiteAlpha = new java.io.File("src/main/resources/suites/suite_alpha.suite");
        final java.io.File suiteBeta = new java.io.File("src/main/resources/suites/suite_beta.suite");
        try {
            suiteAlpha.getParentFile().mkdirs();
            java.nio.file.Files.writeString(suiteAlpha.toPath(), ".test.tests=42\n.test.headless=true\n");
            java.nio.file.Files.writeString(suiteBeta.toPath(), ".test.tests=7\n.test.headless=false\n");

            Base.properties = new TestProperties();
            final Parameters parameters = with("SUITE", "suite_alpha", "OLD_SUITE", "suite_alpha", "TESTS", "42", "HEADLESS", "true");

            assertEquals("42", parameters.getTests());
            assertTrue(parameters.isHeadless());

            parameters.setSuite("suite_beta");

            assertEquals("7", parameters.getTests());
            assertFalse(parameters.isHeadless());
            assertEquals("suite_beta", parameters.getValue(works.lysenko.util.data.enums.ExecutionParameter.OLD_SUITE));
        } finally {
            if (suiteAlpha.exists()) suiteAlpha.delete();
            if (suiteBeta.exists()) suiteBeta.delete();
        }
    }

    @Test
    void testBackdraftClearsStaleParametersWhenOmittedInNewSuite() throws Exception {
        final java.io.File suiteA = new java.io.File("src/main/resources/suites/suite_a.suite");
        final java.io.File suiteB = new java.io.File("src/main/resources/suites/suite_b.suite");
        try {
            suiteA.getParentFile().mkdirs();
            java.nio.file.Files.writeString(suiteA.toPath(), ".test.tests=100\n.tree.completion.weight=3.5\n");
            java.nio.file.Files.writeString(suiteB.toPath(), ".test.headless=false\n");

            Base.properties = new TestProperties();
            final Parameters parameters = with("SUITE", "suite_a", "OLD_SUITE", "suite_a", "TESTS", "100", "COMPLETION_WEIGHT", "3.5");

            parameters.setSuite("suite_b");

            assertEquals("", parameters.getTests());
            assertEquals("suite_b", parameters.getValue(works.lysenko.util.data.enums.ExecutionParameter.OLD_SUITE));
        } finally {
            if (suiteA.exists()) suiteA.delete();
            if (suiteB.exists()) suiteB.delete();
        }
    }
}
