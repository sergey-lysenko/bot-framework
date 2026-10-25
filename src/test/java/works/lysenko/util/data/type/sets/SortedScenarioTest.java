package works.lysenko.util.data.type.sets;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class SortedScenarioTest {

    @Test
    void testInstantiation() {
        final SortedScenario set = new SortedScenario();
        assertNotNull(set.comparator());
    }
}
