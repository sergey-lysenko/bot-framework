package works.lysenko.util.lang.word;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MTest {

    @Test
    void testInstantiation() {
        M instance = new M();
        assertNotNull(instance);
    }

    @Test
    void testConstants() {
        assertNotNull(M.MAKING);
        assertFalse(M.MAKING.isEmpty());
        assertNotNull(M.MANUALLY);
        assertFalse(M.MANUALLY.isEmpty());
        assertNotNull(M.MARGIN);
        assertFalse(M.MARGIN.isEmpty());
        assertNotNull(M.MARGINS);
        assertFalse(M.MARGINS.isEmpty());
        assertNotNull(M.MARKING);
        assertFalse(M.MARKING.isEmpty());
        assertNotNull(M.MASK);
        assertFalse(M.MASK.isEmpty());
        assertNotNull(M.MAXIMUM);
        assertFalse(M.MAXIMUM.isEmpty());
        assertNotNull(M.MERGING);
        assertFalse(M.MERGING.isEmpty());
        assertNotNull(M.MESSAGE);
        assertFalse(M.MESSAGE.isEmpty());
        assertNotNull(M.METHOD);
        assertFalse(M.METHOD.isEmpty());
        assertNotNull(M.MILLISECOND);
        assertFalse(M.MILLISECOND.isEmpty());
        assertNotNull(M.MINIMUM);
        assertFalse(M.MINIMUM.isEmpty());
        assertNotNull(M.MODAL);
        assertFalse(M.MODAL.isEmpty());
        assertNotNull(M.MODEL);
        assertFalse(M.MODEL.isEmpty());
        assertNotNull(M.MODIFIED);
        assertFalse(M.MODIFIED.isEmpty());
        assertNotNull(M.MODIFY);
        assertFalse(M.MODIFY.isEmpty());
        assertNotNull(M.MORE);
        assertFalse(M.MORE.isEmpty());
        assertNotNull(M.MOST);
        assertFalse(M.MOST.isEmpty());
        assertNotNull(M.MUCH);
        assertFalse(M.MUCH.isEmpty());
        assertNotNull(M.MULTI);
        assertFalse(M.MULTI.isEmpty());
    }
}
