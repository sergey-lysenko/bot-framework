package works.lysenko.util.grid;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ProcessorTest {

    @Test
    void processorInstantiation() {
        Processor processor = new Processor(null);
        assertNotNull(processor);
    }
}
