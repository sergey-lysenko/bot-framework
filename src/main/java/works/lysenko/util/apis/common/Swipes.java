package works.lysenko.util.apis.common;

/**
 * The Scrolls interface provides methods for scrolling and swiping through elements on the screen by direct interface
 * pointer manipulations.
 */
@SuppressWarnings({"InterfaceWithOnlyOneDirectInheritor", "BooleanParameter"})
public interface Swipes {

    /**
     * Swipes to the element with the given text.
     *
     * @param text      the text to swipe to
     * @param reduction the inertia of the swipe motion (e.g. speed)
     */
    void swipeToText(final String text, final float reduction);

    /**
     * Swipes to the element with the given text.
     *
     * @param text        the text to swipe to
     * @param lazy        whether to check for the presence of the text on screen before scrolling
     * @param afterScroll a runnable to be executed after the scrolling is performed
     * @param reduction   the inertia of the swipe motion (e.g. speed)
     */
    void swipeToText(final String text, final boolean lazy, final Runnable afterScroll, final float reduction);
}
