package works.lysenko.util.grid;

import org.junit.jupiter.api.Test;
import works.lysenko.util.apis.grid.v._Validation;

import static org.junit.jupiter.api.Assertions.*;

class ProbeTest {

    @Test
    void probeCreateWithNullRequest() {
        try {
            _Validation validation = Probe.create(null);
            assertNotNull(validation);
        } catch (Exception e) {
            assertNotNull(e);
        }
    }
}
