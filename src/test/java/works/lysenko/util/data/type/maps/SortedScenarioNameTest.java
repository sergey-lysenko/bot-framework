package works.lysenko.util.data.type.maps;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class SortedScenarioNameTest {

    @Test
    void testCaseInsensitiveKey() {
        final SortedScenarioName map = new SortedScenarioName();
        assertNotNull(map.comparator());

        map.put("Alpha", null);
        assertEquals(1, map.size());
        assertEquals(true, map.containsKey("alpha"));
    }
}
