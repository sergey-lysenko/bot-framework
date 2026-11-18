package works.lysenko.util.data.records;

import org.apache.commons.math3.fraction.Fraction;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class SlackTest {

    @Test
    void testSlFactories() {
        final RangedMargin rm = RangedMargin.rm();
        final Slack slack1 = Slack.sl(rm);
        assertEquals(rm, slack1.margin());
        assertNull(slack1.border());

        final Slack slack2 = Slack.sl(rm, Fraction.ONE_HALF);
        assertEquals(rm, slack2.margin());
        assertEquals(Fraction.ONE_HALF, slack2.border());
    }

    @Test
    void testDoubleValue() {
        final RangedMargin rm = RangedMargin.rm(Fraction.ONE_HALF);
        final Slack slack = Slack.sl(rm, Fraction.ONE);
        assertEquals(0.5, slack.doubleValue(Fraction.ONE_HALF), 0.001);

        final Slack nullMarginSlack = Slack.sl(null, Fraction.ONE);
        assertEquals(0.0, nullMarginSlack.doubleValue(Fraction.ONE_HALF), 0.001);
    }

    @Test
    void testToString() {
        final RangedMargin rm = RangedMargin.rm(Fraction.ONE_HALF);
        final Slack slack = Slack.sl(rm, Fraction.ONE_HALF);
        assertNotNull(slack.toString());
    }
}
