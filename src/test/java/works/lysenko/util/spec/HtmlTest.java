package works.lysenko.util.spec;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class HtmlTest {

    @Test
    void testHtmlTagsAndSequences() {
        assertEquals("<body>", Html.BODY_);
        assertEquals("<table>", Html.TABLE_);
        assertEquals("<tr>", Html.TR_);
        assertEquals("<td>", Html.TD_);
        assertEquals("</table>", Html._TABLE);
        assertEquals("</td>", Html._TD);
        assertEquals("</tr>", Html._TR);
        assertEquals("<h1>", Html.H_1_);
        assertEquals("</h1>", Html._H_1);
        assertEquals("<h2>", Html.H_2_);
        assertEquals("</h2>", Html._H_2);
        assertNotNull(Html.TABLE_TR_TH_);
        assertNotNull(Html.HTML_HEAD_TITLE_);
        assertNotNull(Html._TABLE_BODY_HTML);
        assertNotNull(new Html());
    }
}

