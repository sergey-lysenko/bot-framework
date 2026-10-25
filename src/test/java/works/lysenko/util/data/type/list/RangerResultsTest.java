package works.lysenko.util.data.type.list;

import org.junit.jupiter.api.Test;
import works.lysenko.util.data.enums.RangerResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RangerResultsTest {

    @Test
    void testRangerResults() {
        final RangerResults results = new RangerResults(RangerResult.OK);
        assertFalse(results.isEmpty());
        assertTrue(results.isPassed());
        assertEquals(1, results.size());

        results.add(RangerResult.NOT_OK);
        assertFalse(results.isPassed());
        assertNotNull(results.render());
        assertNotNull(results.basedOn());
    }
}
