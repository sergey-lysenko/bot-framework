package works.lysenko.util.data.type.sets;

import org.junit.jupiter.api.Test;
import works.lysenko.util.data.enums.GridValidationResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GridValidationResultsTest {

    @Test
    void testGridValidationResults() {
        final GridValidationResults results = GridValidationResults.mvr(GridValidationResult.OK);
        assertFalse(results.isEmpty());
        assertTrue(results.isPassed());
        assertEquals(1, results.size());

        assertNotNull(results.render());
        assertNotNull(results.names());
    }
}
