package works.lysenko.util;

import static works.lysenko.util.chrs.___.TAB;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static works.lysenko.util.chrs.___.AND;
import static works.lysenko.util.chrs.___.COM;
import static works.lysenko.util.chrs.___.DOTS;
import static works.lysenko.util.chrs.____.*;
import static works.lysenko.util.data.strs.Bind.b;
import static works.lysenko.util.data.strs.Bind.d;
import static works.lysenko.util.data.strs.Case.c;
import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.lang.word.A.ANDROID;
import static works.lysenko.util.lang.word.B.BUTTON;
import static works.lysenko.util.lang.word.C.CHILD;
import static works.lysenko.util.lang.word.C.CONTENT;
import static works.lysenko.util.lang.word.F.FRAME;
import static works.lysenko.util.lang.word.G.GROUP;
import static works.lysenko.util.lang.word.I.INTERNAL;
import static works.lysenko.util.lang.word.L.LAYOUT;
import static works.lysenko.util.lang.word.L.LINEAR;
import static works.lysenko.util.lang.word.R.RECYCLER;
import static works.lysenko.util.lang.word.S.SCROLL;
import static works.lysenko.util.lang.word.W.WIDGET;
import static works.lysenko.util.spec.Symbols.*;

/**
 * Class containing constants for various Android UI elements and layouts
 */
@SuppressWarnings({"MissingJavadoc", "StaticMethodOnlyUsedInOneClass", "unused"})
public record Constants() {

    // Tier 1
    public static final String EDIT_TEXT = s(c(EDIT), c(TEXT));
    public static final String RECYCLER_VIEW = s(c(RECYCLER), c(VIEW));
    public static final String TAB_HOST = s(c(TAB), c(HOST));
    public static final String SCROLL_VIEW = s(c(SCROLL), c(VIEW));
    public static final String TEXT_VIEW = s(c(TEXT), c(VIEW));
    public static final String _ANDROID_VIEW = s(_SLASH_, ANDROID, _DOT_, VIEW, _DOT_);
    public static final String _AND_CHILD = b(EMPTY, DOTS, AND, CHILD);
    public static final String CONTENT_DESC = d(CONTENT, DESC);

    // Tier 2
    private static final String FRAME_LAYOUT = s(c(FRAME), c(LAYOUT));
    private static final String LINEAR_LAYOUT = s(c(LINEAR), c(LAYOUT));
    private static final String VIEW_GROUP = s(c(VIEW), c(GROUP));

    // Tier 3
    private static final String ANDROID_INTERNAL_WIDGET = s(b(_DOT_, COM, ANDROID, INTERNAL, WIDGET), _DOT_);
    public static final String _ANDROID_INTERNAL_WIDGET = s(_SLASH_, _SLASH_, ANDROID_INTERNAL_WIDGET);
    public static final String _ANDROID_RECYCLER_VIEW = s(_ANDROID_INTERNAL_WIDGET, RECYCLER_VIEW);
    private static final String ANDROID_VIEW = s(ANDROID, _DOT_, VIEW, _DOT_);
    public static final String ANDROID_VIEW_VIEW_GROUP = s(ANDROID_VIEW, VIEW_GROUP);
    private static final String ANDROID_WIDGET = s(ANDROID, _DOT_, WIDGET, _DOT_);
    public static final String ANDROID_WIDGET_FRAME_LAYOUT = s(ANDROID_WIDGET, FRAME_LAYOUT);
    public static final String ANDROID_WIDGET_LINEAR_LAYOUT = s(ANDROID_WIDGET, LINEAR_LAYOUT);
    public static final String ANDROID_WIDGET_TEXT_VIEW = s(ANDROID_WIDGET, TEXT_VIEW);

    // Tier 4
    public static final String __ANDROID_VIEW = s(_SLASH_, _SLASH_, ANDROID_VIEW);
    public static final String __ANDROID_WIDGET = s(_SLASH_, _SLASH_, ANDROID_WIDGET);

    // Tier 5
    public static final String _ANDROID_WIDGET_BUTTON = s(__ANDROID_WIDGET, c(BUTTON));
    public static final String _ANDROID_WIDGET_SCROLLVIEW = s(__ANDROID_WIDGET, SCROLL_VIEW);
    public static final String _ANDROID_WIDGET_TEXTVIEW = s(__ANDROID_WIDGET, TEXT_VIEW);
}
