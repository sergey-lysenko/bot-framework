package works.lysenko.util.func.math;

import org.apache.commons.math3.distribution.EnumeratedDistribution;
import org.junit.jupiter.api.Test;
import works.lysenko.util.data.records.KeyValue;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class EnumeratedDistributionsTest {

    @Test
    void testEnumeratedDistributionsInstantiation() {
        EnumeratedDistributions<String> distributions = new EnumeratedDistributions<>();
        assertNotNull(distributions);
    }

    @Test
    void testEnumDis() {
        List<KeyValue<String, Double>> list = List.of(new KeyValue<>("A", 1.0), new KeyValue<>("B", 2.0));
        EnumeratedDistribution<String> dist = EnumeratedDistributions.enumDis(list);
        assertNotNull(dist);
    }
}
