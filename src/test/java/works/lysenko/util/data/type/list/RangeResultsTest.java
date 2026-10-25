package works.lysenko.util.data.type.list;

import org.junit.jupiter.api.Test;
import works.lysenko.util.data.enums.RangeResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RangeResultsTest {

    @Test
    void testRangeResults() {
        final RangeResults results = new RangeResults();
        assertTrue(results.isEmpty());
        assertTrue(results.isPassed());

        results.add(RangeResult.OK);
        assertFalse(results.isEmpty());
        assertEquals(1, results.size());
        assertTrue(results.isPassed());

        results.add(RangeResult.NOT_OK);
        assertFalse(results.isPassed());
        assertNotNull(results.render());
        assertNotNull(results.basedOn());
    }
}
