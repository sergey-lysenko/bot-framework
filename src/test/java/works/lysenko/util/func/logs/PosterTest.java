package works.lysenko.util.func.logs;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class PosterTest {

    @Test
    void testPosterInstantiation() {
        Poster poster = new Poster();
        assertNotNull(poster);
    }
}
