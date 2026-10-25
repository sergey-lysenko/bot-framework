package works.lysenko.util.grid.validation;

import org.junit.jupiter.api.Test;
import works.lysenko.util.data.type.sets.GridValidationResults;

import static org.junit.jupiter.api.Assertions.*;

class RoutinesTest {

    @Test
    void testValidateRequestWithNull() {
        GridValidationResults results = Routines.validateRequest(null);
        assertNotNull(results);
        assertFalse(results.isPassed());
    }

    @Test
    void testRoutinesInstantiation() {
        Routines routines = new Routines();
        assertNotNull(routines);
    }
}
