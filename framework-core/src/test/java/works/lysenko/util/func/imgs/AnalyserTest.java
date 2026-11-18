package works.lysenko.util.func.imgs;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class AnalyserTest {

    @Test
    void testAnalyse() {
        assertNotNull(Analyser.getMaximallyFarAwayPoints(10));
    }
}
