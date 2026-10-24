package works.lysenko.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ConstantsTest {

    @Test
    void testConstantsNotNullAndNotEmpty() {
        assertNotNull(Constants.EDIT_TEXT);
        assertFalse(Constants.EDIT_TEXT.isEmpty());

        assertNotNull(Constants.RECYCLER_VIEW);
        assertFalse(Constants.RECYCLER_VIEW.isEmpty());

        assertNotNull(Constants.TAB_HOST);
        assertFalse(Constants.TAB_HOST.isEmpty());

        assertNotNull(Constants.SCROLL_VIEW);
        assertFalse(Constants.SCROLL_VIEW.isEmpty());

        assertNotNull(Constants.TEXT_VIEW);
        assertFalse(Constants.TEXT_VIEW.isEmpty());

        assertNotNull(Constants._ANDROID_VIEW);
        assertFalse(Constants._ANDROID_VIEW.isEmpty());

        assertNotNull(Constants._AND_CHILD);
        assertFalse(Constants._AND_CHILD.isEmpty());

        assertNotNull(Constants.CONTENT_DESC);
        assertFalse(Constants.CONTENT_DESC.isEmpty());

        assertNotNull(Constants._ANDROID_INTERNAL_WIDGET);
        assertFalse(Constants._ANDROID_INTERNAL_WIDGET.isEmpty());

        assertNotNull(Constants._ANDROID_RECYCLER_VIEW);
        assertFalse(Constants._ANDROID_RECYCLER_VIEW.isEmpty());

        assertNotNull(Constants.ANDROID_VIEW_VIEW_GROUP);
        assertFalse(Constants.ANDROID_VIEW_VIEW_GROUP.isEmpty());

        assertNotNull(Constants.ANDROID_WIDGET_FRAME_LAYOUT);
        assertFalse(Constants.ANDROID_WIDGET_FRAME_LAYOUT.isEmpty());

        assertNotNull(Constants.ANDROID_WIDGET_LINEAR_LAYOUT);
        assertFalse(Constants.ANDROID_WIDGET_LINEAR_LAYOUT.isEmpty());

        assertNotNull(Constants.ANDROID_WIDGET_TEXT_VIEW);
        assertFalse(Constants.ANDROID_WIDGET_TEXT_VIEW.isEmpty());

        assertNotNull(Constants.__ANDROID_VIEW);
        assertFalse(Constants.__ANDROID_VIEW.isEmpty());

        assertNotNull(Constants.__ANDROID_WIDGET);
        assertFalse(Constants.__ANDROID_WIDGET.isEmpty());

        assertNotNull(Constants._ANDROID_WIDGET_BUTTON);
        assertFalse(Constants._ANDROID_WIDGET_BUTTON.isEmpty());

        assertNotNull(Constants._ANDROID_WIDGET_SCROLLVIEW);
        assertFalse(Constants._ANDROID_WIDGET_SCROLLVIEW.isEmpty());

        assertNotNull(Constants._ANDROID_WIDGET_TEXTVIEW);
        assertFalse(Constants._ANDROID_WIDGET_TEXTVIEW.isEmpty());

        assertNotNull(Constants._S0_);
        assertFalse(Constants._S0_.isEmpty());

        assertNotNull(Constants._S1_);
        assertFalse(Constants._S1_.isEmpty());

        assertNotNull(Constants._S2_);
        assertFalse(Constants._S2_.isEmpty());
    }

    @Test
    void testSpecificConstantValues() {
        assertEquals("EditText", Constants.EDIT_TEXT);
        assertEquals("RecyclerView", Constants.RECYCLER_VIEW);
        assertEquals("TabHost", Constants.TAB_HOST);
        assertEquals("ScrollView", Constants.SCROLL_VIEW);
        assertEquals("TextView", Constants.TEXT_VIEW);
    }

    @Test
    void testRecordInstantiation() {
        Constants constants = new Constants();
        assertNotNull(constants);
    }
}
