package works.lysenko.util.lang.word;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WTest {

    @Test
    void testInstantiation() {
        W instance = new W();
        assertNotNull(instance);
    }

    @Test
    void testConstants() {
        assertNotNull(W.WAITING);
        assertFalse(W.WAITING.isEmpty());
        assertNotNull(W.WARNING);
        assertFalse(W.WARNING.isEmpty());
        assertNotNull(W.WEIGHT);
        assertFalse(W.WEIGHT.isEmpty());
        assertNotNull(W.WEIGHTS);
        assertFalse(W.WEIGHTS.isEmpty());
        assertNotNull(W.WHETHER);
        assertFalse(W.WHETHER.isEmpty());
        assertNotNull(W.WHICH);
        assertFalse(W.WHICH.isEmpty());
        assertNotNull(W.WHILE);
        assertFalse(W.WHILE.isEmpty());
        assertNotNull(W.WIDER);
        assertFalse(W.WIDER.isEmpty());
        assertNotNull(W.WIDGET);
        assertFalse(W.WIDGET.isEmpty());
        assertNotNull(W.WIDTH);
        assertFalse(W.WIDTH.isEmpty());
        assertNotNull(W.WINDOW);
        assertFalse(W.WINDOW.isEmpty());
        assertNotNull(W.WITHIN);
        assertFalse(W.WITHIN.isEmpty());
        assertNotNull(W.WITHOUT);
        assertFalse(W.WITHOUT.isEmpty());
        assertNotNull(W.WORKAROUND);
        assertFalse(W.WORKAROUND.isEmpty());
        assertNotNull(W.WRAPPER);
        assertFalse(W.WRAPPER.isEmpty());
        assertNotNull(W.WRITE);
        assertFalse(W.WRITE.isEmpty());
        assertNotNull(W.WRITER);
        assertFalse(W.WRITER.isEmpty());
        assertNotNull(W.WRITING);
        assertFalse(W.WRITING.isEmpty());
        assertNotNull(W.WRONG);
        assertFalse(W.WRONG.isEmpty());
    }
}
