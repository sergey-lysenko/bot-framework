package works.lysenko.util.func.logs;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class ColumnsTest {

    @Test
    void testMinimalRows() {
        List<String> rows = Columns.minimalRows(List.of("a", "b"), 10, 2);
        assertNotNull(rows);
    }
}
