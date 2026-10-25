package works.lysenko.util.spec;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class SurrogatesTest {

    @Test
    void testSurrogatesNotNullAndNotEmpty() {
        assertNotNull(Surrogates.HOUSE);
        assertFalse(Surrogates.HOUSE.isEmpty());
        assertNotNull(Surrogates.GRADUATION_CAP);
        assertNotNull(Surrogates.BRIEFCASE);
        assertNotNull(Surrogates.STETHOSCOPE);
        assertNotNull(Surrogates.AUTOMOBILE);
        assertNotNull(Surrogates.SCISSORS);
        assertNotNull(Surrogates.BACKPACK);
        assertNotNull(Surrogates.PLATE);
        assertNotNull(Surrogates.WEIGHT_LIFTER);
        assertNotNull(Surrogates.ARTIST_PALETTE);
        assertNotNull(Surrogates.MICROPHONE);
        assertNotNull(Surrogates.DELIVERY_TRUCK);
        assertNotNull(new Surrogates());
    }
}

