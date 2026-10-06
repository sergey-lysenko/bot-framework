package works.lysenko.base;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import works.lysenko.Base;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Precedence rules of execution parameters (regression guard for ".all.leafs.count" being ignored).
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
        assertEquals(1, parameters.getAllLeafsCount());
        assertFalse(parameters.isHeadless());
    }

    @Test
    void countAboveOneImpliesAllLeafs() {

        final Parameters parameters = with("ALL_LEAFS_COUNT", "3");
        assertEquals(3, parameters.getAllLeafsCount());
        assertTrue(parameters.isAllLeafs());
    }

    @Test
    void countAboveOneOverridesExplicitFalse() {

        final Parameters parameters = with("ALL_LEAFS", "false", "ALL_LEAFS_COUNT", "3");
        assertTrue(parameters.isAllLeafs());
        assertEquals(3, parameters.getAllLeafsCount());
    }

    @Test
    void explicitAllLeafsWithSinglePass() {

        final Parameters parameters = with("ALL_LEAFS", "true", "ALL_LEAFS_COUNT", "1");
        assertTrue(parameters.isAllLeafs());
        assertEquals(1, parameters.getAllLeafsCount());
    }

    @Test
    void explicitFalseWithSinglePass() {

        final Parameters parameters = with("ALL_LEAFS", "false", "ALL_LEAFS_COUNT", "1");
        assertFalse(parameters.isAllLeafs());
    }

    @Test
    void emptyValuesFallBackToDefaults() {

        final Parameters parameters = with("ALL_LEAFS", "", "ALL_LEAFS_COUNT", "");
        assertFalse(parameters.isAllLeafs());
        assertEquals(1, parameters.getAllLeafsCount());
    }

    @Test
    void countIsClampedToAtLeastOne() {

        assertEquals(1, with("ALL_LEAFS_COUNT", "0").getAllLeafsCount());
        assertEquals(1, with("ALL_LEAFS_COUNT", "-5").getAllLeafsCount());
    }

    @Test
    void malformedCountIsIgnored() {

        final Parameters parameters = with("ALL_LEAFS_COUNT", "many");
        assertEquals(1, parameters.getAllLeafsCount());
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
}
