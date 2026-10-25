package works.lysenko.util.lang.word;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FTest {

    @Test
    void testInstantiation() {
        F instance = new F();
        assertNotNull(instance);
    }

    @Test
    void testConstants() {
        assertNotNull(F.FAILED);
        assertFalse(F.FAILED.isEmpty());
        assertNotNull(F.FAILING);
        assertFalse(F.FAILING.isEmpty());
        assertNotNull(F.FAILURE);
        assertFalse(F.FAILURE.isEmpty());
        assertNotNull(F.FAILURES);
        assertFalse(F.FAILURES.isEmpty());
        assertNotNull(F.FALSE);
        assertFalse(F.FALSE.isEmpty());
        assertNotNull(F.FENCE);
        assertFalse(F.FENCE.isEmpty());
        assertNotNull(F.FENCES);
        assertFalse(F.FENCES.isEmpty());
        assertNotNull(F.FFMPEG);
        assertFalse(F.FFMPEG.isEmpty());
        assertNotNull(F.FILTER);
        assertFalse(F.FILTER.isEmpty());
        assertNotNull(F.FINAL);
        assertFalse(F.FINAL.isEmpty());
        assertNotNull(F.FINDING);
        assertFalse(F.FINDING.isEmpty());
        assertNotNull(F.FINGER);
        assertFalse(F.FINGER.isEmpty());
        assertNotNull(F.FIXED);
        assertFalse(F.FIXED.isEmpty());
        assertNotNull(F.FORWARD);
        assertFalse(F.FORWARD.isEmpty());
        assertNotNull(F.FOUND);
        assertFalse(F.FOUND.isEmpty());
        assertNotNull(F.FRACTION);
        assertFalse(F.FRACTION.isEmpty());
        assertNotNull(F.FRAME);
        assertFalse(F.FRAME.isEmpty());
        assertNotNull(F.FULLSCREEN);
        assertFalse(F.FULLSCREEN.isEmpty());
        assertNotNull(F.FULLY);
        assertFalse(F.FULLY.isEmpty());
    }
}
