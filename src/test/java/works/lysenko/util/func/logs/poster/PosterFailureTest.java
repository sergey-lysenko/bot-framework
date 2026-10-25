package works.lysenko.util.func.logs.poster;

import org.junit.jupiter.api.Test;
import works.lysenko.util.data.enums.Severity;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class PosterFailureTest {

    @Test
    void testFailureRecord() {
        Failure failure = new Failure(Severity.S0, "type", "desc", null, null, null);
        assertNotNull(failure);
    }
}
