package works.lysenko.util.grid;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FieldsTest {

    @Test
    void fieldsConstantsNotNull() {
        assertNotNull(Fields.CDI);
        assertNotNull(Fields.BDF);
        assertNotNull(Fields.BDT);
        assertNotNull(Fields.CDA);
        assertNotNull(Fields.CDB);
        assertNotNull(Fields.CDC);
        assertNotNull(Fields.CDD);
        assertNotNull(Fields.CDIB);
        assertNotNull(Fields.CDIH);
        assertNotNull(Fields.CDIS);
        assertNotNull(Fields.HDF);
        assertNotNull(Fields.HDT);
        assertNotNull(Fields.PDI);
        assertNotNull(Fields.PDM);
        assertNotNull(Fields.SDF);
        assertNotNull(Fields.SDT);

        Fields fields = new Fields();
        assertNotNull(fields);
    }
}
