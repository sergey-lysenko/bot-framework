package works.lysenko.util.func.logs.trace;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class TraceDataTest {

    @Test
    void testDataInstantiation() {
        Data data = new Data();
        assertNotNull(data);
    }
}
