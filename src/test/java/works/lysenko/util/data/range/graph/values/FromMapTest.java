package works.lysenko.util.data.range.graph.values;

import org.apache.commons.math3.fraction.Fraction;
import org.junit.jupiter.api.Test;
import works.lysenko.util.apis.grid.v._ValuedRangeResult;
import works.lysenko.util.data.records.Slack;
import works.lysenko.util.grid.record.graph.Parameters;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class FromMapTest {

    @Test
    void testGetGraphParameters() {

        final Map<String, _ValuedRangeResult> shares = new HashMap<>();
        final Fraction max = Fraction.ONE;

        final FromMap fromMap = new FromMap(shares, max);
        assertEquals(shares, fromMap.shares());
        assertEquals(max, fromMap.max());

        final Slack slack = new Slack(Fraction.ZERO);
        final Parameters params = fromMap.getGraphParameters(slack);
        assertNotNull(params);
        assertEquals(slack, params.slack());
    }
}
