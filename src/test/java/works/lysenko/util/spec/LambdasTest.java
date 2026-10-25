package works.lysenko.util.spec;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class LambdasTest {

    @Test
    void testNopLambda() {
        assertNotNull(Lambdas.NOP);
        Lambdas.NOP.run();
        assertNotNull(new Lambdas());
    }
}

