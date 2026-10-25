package works.lysenko.util.func.imgs;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class DistributionTest {

    @Test
    void testDistributionInstantiation() {
        Distribution distribution = new Distribution();
        assertNotNull(distribution);
    }
}
