package works.lysenko.util.lang.word;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PTest {

    @Test
    void testInstantiation() {
        P instance = new P();
        assertNotNull(instance);
    }

    @Test
    void testConstants() {
        assertNotNull(P.PACKAGE);
        assertFalse(P.PACKAGE.isEmpty());
        assertNotNull(P.PAIRS);
        assertFalse(P.PAIRS.isEmpty());
        assertNotNull(P.PALETTE);
        assertFalse(P.PALETTE.isEmpty());
        assertNotNull(P.PARAMETER);
        assertFalse(P.PARAMETER.isEmpty());
        assertNotNull(P.PARAMETERS);
        assertFalse(P.PARAMETERS.isEmpty());
        assertNotNull(P.PARSE);
        assertFalse(P.PARSE.isEmpty());
        assertNotNull(P.PARSER);
        assertFalse(P.PARSER.isEmpty());
        assertNotNull(P.PARTIAL);
        assertFalse(P.PARTIAL.isEmpty());
        assertNotNull(P.PASSED);
        assertFalse(P.PASSED.isEmpty());
        assertNotNull(P.PATHS);
        assertFalse(P.PATHS.isEmpty());
        assertNotNull(P.PAUSE);
        assertFalse(P.PAUSE.isEmpty());
        assertNotNull(P.PAUSED);
        assertFalse(P.PAUSED.isEmpty());
        assertNotNull(P.PENULTIMATE);
        assertFalse(P.PENULTIMATE.isEmpty());
        assertNotNull(P.PERFORMED);
        assertFalse(P.PERFORMED.isEmpty());
        assertNotNull(P.PERFORMING);
        assertFalse(P.PERFORMING.isEmpty());
        assertNotNull(P.PERSISTED);
        assertFalse(P.PERSISTED.isEmpty());
        assertNotNull(P.PERSISTING);
        assertFalse(P.PERSISTING.isEmpty());
        assertNotNull(P.PIXEL);
        assertFalse(P.PIXEL.isEmpty());
        assertNotNull(P.PIXELS);
        assertFalse(P.PIXELS.isEmpty());
        assertNotNull(P.PLATFORM);
        assertFalse(P.PLATFORM.isEmpty());
        assertNotNull(P.PLATFORMS);
        assertFalse(P.PLATFORMS.isEmpty());
        assertNotNull(P.POINTS);
        assertFalse(P.POINTS.isEmpty());
        assertNotNull(P.POLYCHROMY);
        assertFalse(P.POLYCHROMY.isEmpty());
        assertNotNull(P.POSITION);
        assertFalse(P.POSITION.isEmpty());
        assertNotNull(P.POSSIBLE);
        assertFalse(P.POSSIBLE.isEmpty());
        assertNotNull(P.POSTFLIGHT);
        assertFalse(P.POSTFLIGHT.isEmpty());
        assertNotNull(P.PREEMPTIVELY);
        assertFalse(P.PREEMPTIVELY.isEmpty());
        assertNotNull(P.PREFLIGHT);
        assertFalse(P.PREFLIGHT.isEmpty());
        assertNotNull(P.PRESENCE);
        assertFalse(P.PRESENCE.isEmpty());
        assertNotNull(P.PREVIEW);
        assertFalse(P.PREVIEW.isEmpty());
        assertNotNull(P.PROBABILITY);
        assertFalse(P.PROBABILITY.isEmpty());
        assertNotNull(P.PROBLEM);
        assertFalse(P.PROBLEM.isEmpty());
        assertNotNull(P.PROCEED);
        assertFalse(P.PROCEED.isEmpty());
        assertNotNull(P.PROCESS);
        assertFalse(P.PROCESS.isEmpty());
        assertNotNull(P.PROCESSING);
        assertFalse(P.PROCESSING.isEmpty());
        assertNotNull(P.PROPERTIES);
        assertFalse(P.PROPERTIES.isEmpty());
        assertNotNull(P.PROPERTY);
        assertFalse(P.PROPERTY.isEmpty());
        assertNotNull(P.PROPOSED);
        assertFalse(P.PROPOSED.isEmpty());
        assertNotNull(P.PROVIDED);
        assertFalse(P.PROVIDED.isEmpty());
        assertNotNull(P.PUBLIC);
        assertFalse(P.PUBLIC.isEmpty());
    }
}
