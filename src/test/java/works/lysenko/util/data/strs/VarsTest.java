package works.lysenko.util.data.strs;

import org.junit.jupiter.api.Test;
import works.lysenko.util.data.records.KeyValue;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class VarsTest {

    @Test
    void testA() {
        assertEquals("{k=v}", Vars.a("k", "v"));
        assertEquals("{k=10}", Vars.a("k", 10));
        assertEquals("{k=true}", Vars.a("k", true));
        assertEquals("{k=v},", Vars.a("k", "v", ','));

        final List<KeyValue<?, ?>> list = List.of(KeyValue.kv("a", "1"), KeyValue.kv("b", "2"));
        assertEquals("{a=1}; {b=2}", Vars.a(list, "; "));
    }
}
