package works.lysenko.util.func.type;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class RandomListElementTest {

    @Test
    void testRandomListElementInstantiation() {
        RandomListElement<String> randomListElement = new RandomListElement<>(0, "a");
        assertNotNull(randomListElement);
    }
}
