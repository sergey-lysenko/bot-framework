package works.lysenko.util.func.grid;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class RenderersTest {

    @Test
    void testRenderersInstantiation() {
        Renderers renderers = Renderers.r(null, null, null);
        assertNotNull(renderers);
    }
}
