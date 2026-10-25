package works.lysenko.util.grid.record.meta;

import org.junit.jupiter.api.Test;
import works.lysenko.util.data.records.Noun;

import static org.junit.jupiter.api.Assertions.*;

class SubjectTest {

    @Test
    void testSubjectInstantiation() {
        Noun noun = new Noun("item", "items");
        Subject subject = new Subject(noun, Method.COMPULSIVE);
        assertEquals(noun, subject.noun());
        assertEquals(Method.COMPULSIVE, subject.method());
    }
}
