package works.lysenko.util.grid.record.rash;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SharesDataTest {

    @Test
    void testSharesDataInstantiation() {
        SharesData<String> sharesData = new SharesData<>("value", 1, 10.0, 100);
        assertEquals("value", sharesData.value());
        assertEquals(1, sharesData.i());
        assertEquals(10.0, sharesData.max());
        assertEquals(100, sharesData.width());
    }
}
