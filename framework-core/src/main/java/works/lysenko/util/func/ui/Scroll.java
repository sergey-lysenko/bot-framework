package works.lysenko.util.func.ui;

import works.lysenko.util.apis.exception.unchecked.SwipeException;
import works.lysenko.util.data.type.RelativePosition;
import works.lysenko.util.func.ui.scroll.Data;

import java.awt.Point;
import java.awt.image.BufferedImage;

import static java.util.Objects.isNull;
import static org.apache.commons.math3.fraction.Fraction.ONE_HALF;
import static works.lysenko.Base.exec;
import static works.lysenko.util.func.type.Numbers.random;
import static works.lysenko.util.func.ui.Locators.nthScrollView;
import static works.lysenko.util.func.ui.scroll.Data.*;
import static works.lysenko.util.spec.Waits.SHORT_WAIT_FROM;
import static works.lysenko.util.spec.Waits.SHORT_WAIT_TO;

/**
 * The Scroll class provides static methods to perform scrolling and swiping actions on elements.
 */
@SuppressWarnings({"StaticMethodOnlyUsedInOneClass", "ClassIndependentOfModule", "unused"})
public record Scroll() {

    /**
     * Returns the default swiper locator by calling the {@code nthScrollView} method with a parameter of 1.
     *
     * @return the locator string for the default swiper.
     */
    @SuppressWarnings("WeakerAccess")
    public static String defaultSwiperLocator() {

        return nthScrollView(1);
    }

    /**
     * Scrolls the specified scroller until the target element or limit element is present, with a maximum number of scrolls
     * and a reduction factor.
     *
     * @param scroller   the locator of the element to scroll
     * @param target     the locator of the target element to find
     * @param limit      the locator of the limit element to check if reached
     * @param maxScrolls the maximum number of scrolls to perform before stopping
     * @param reduction  the reduction factor for each scroll action (1.0 for full length, 2.0 for half the way, 0.5 for
     *                   twice the distance)
     * @throws SwipeException if the swipe action does not cause a change in the image
     */
    @SuppressWarnings({"MethodCallInLoopCondition", "ValueOfIncrementOrDecrementUsed", "unused"})
    public static void scrollTo(final String scroller, final String target, final String limit, final int maxScrolls,
                                final float reduction) {

        int scrolls = maxScrolls;
        while (!exec.isPresent(target) && !exec.isPresent(limit) && (0 < scrolls--)) {
            swipeVertically(scroller, reduction);
        }
    }

    /**
     * Performs a swipe action with a specified reduction factor.
     *
     * @param reduction the reduction factor for each swipe action (1.0 for full length, 2.0 for half the way, 0.5 for twice
     *                 the distance)
     * @throws SwipeException if the swipe action does not cause a change in the image
     */
    @SuppressWarnings({"NumericCastThatLosesPrecision", "OverloadedMethodsWithSameNumberOfParameters", "unused"})
    public static void swipe(final double reduction) {

        swipe((float) reduction);
    }

    /**
     * Performs a vertical swipe action using the default swiper locator.
     * The swipe action is initiated with a reduction factor of -1.
     */
    public static void swipe() {

        swipeVertically(defaultSwiperLocator(), -1);
    }

    /**
     * Performs a vertical swipe action using the default swiper locator.
     * This method initiates the swipe with a reduction factor of 1.
     */
    public static void swipeBack() {

        swipeVertically(defaultSwiperLocator(), 1);
    }

    /**
     * Performs a swipe action with a specified reduction factor.
     *
     * @param reduction the reduction factor for each swipe action (1.0 for full length, 2.0 for half the way, 0.5 for twice
     *                 the distance)
     * @throws SwipeException if the swipe action does not cause a change in the image
     */
    @SuppressWarnings({"OverloadedMethodsWithSameNumberOfParameters", "WeakerAccess"})
    public static void swipe(final float reduction) {

        swipeVertically(defaultSwiperLocator(), reduction);
    }

    /**
     * Swipes the screen vertically with a specified reduction factor.
     *
     * @param position  the relative position of the element to the scroller
     * @param reduction the reduction factor for each swipe action (1.0 for full length, 2.0 for half the way, 0.5 for twice
     *                 the distance)
     */
    @SuppressWarnings("unused")
    public static void swipe(final RelativePosition position, final float reduction) {

        swipeVertically(defaultSwiperLocator(), position, reduction);
    }

    /**
     * Performs a swipe action on the specified element with the given reduction factor.
     *
     * @param locator      the locator of the element to perform the swipe action
     * @param position     the relative position of the element to the scroller (optional, nullable)
     * @param reduction    the reduction factor for each swipe action (1.0 for full length, 2.0 for half the way, 0.5 for
     *                     twice the distance)
     * @param horizontally set to true to perform a horizontal swipe, false for vertical swipe
     */
    @SuppressWarnings("StandardVariableNames")
    private static void swipe(final String locator, final RelativePosition position, final float reduction,
                              final boolean horizontally) {

        final RelativePosition relativePosition = (isNull(position)) ? RelativePosition.rp(ONE_HALF, ONE_HALF) : position;
        final int duration = random(SHORT_WAIT_FROM, SHORT_WAIT_TO);
        final Data d = getData(locator);
        final Point s = getStartingPoint(d, relativePosition);
        final Point t = getTargetPoint(reduction, horizontally, s, d, duration);
        final BufferedImage before = getBeforeScreenshot(s, t, duration);
        performSwipingAction(d, duration, s, t);
        getAfterScreenshot(before);
    }

    /**
     * Performs a horizontal swipe action on the specified element with the given reduction factor.
     *
     * @param locator   the locator of the element to swipe horizontally
     * @param reduction the reduction factor for each swipe action (1.0 for full length, 2.0 for half the way, 0.5 for twice
     *                 the distance)
     * @throws SwipeException if the swipe action does not cause a change in the image
     */
    @SuppressWarnings("unused")
    public static void swipe(final String locator, final float reduction) {

        swipe(locator, null, reduction, false);
    }

    /**
     * Performs a horizontal swipe action on the specified element with the given reduction factor.
     *
     * @param locator   the locator of the element to swipe horizontally
     * @param position  the relative position of the element to the viewport
     * @param reduction the reduction factor for each swipe action (1.0 for full length, 2.0 for half the way, 0.5 for twice
     *                 the distance)
     * @throws SwipeException if the swipe action does not cause a change in the image
     */
    @SuppressWarnings("unused")
    public static void swipeHorizontally(final String locator, final RelativePosition position, final float reduction) {

        swipe(locator, position, reduction, true);
    }

    /**
     * Performs a horizontal swipe action on the specified element with the given reduction factor.
     *
     * @param locator   the locator of the element to swipe horizontally
     * @param reduction the reduction factor for each swipe action (1.0 for full length, 2.0 for half the way, 0.5 for twice
     *                 the distance)
     * @throws SwipeException if the swipe action does not cause a change in the image
     */
    @SuppressWarnings("unused")
    public static void swipeHorizontally(final String locator, final float reduction) {

        swipe(locator, null, reduction, true);
    }

    /**
     * Perform a vertical swipe action on the specified element with the given reduction factor.
     *
     * @param locator   the locator of the element to swipe vertically
     * @param position  the relative position of the element to the viewport
     * @param reduction the reduction factor for each swipe action (1.0 for full length, 2.0 for half the way, 0.5 for twice
     *                 the distance)
     * @throws SwipeException if the swipe action does not cause a change in the image
     */
    private static void swipeVertically(final String locator, final RelativePosition position, final float reduction) {

        swipe(locator, position, reduction, false);
    }

    /**
     * Perform a vertical swipe action on the specified element with the given reduction factor.
     *
     * @param locator   the locator of the element to swipe vertically
     * @param reduction the reduction factor for each swipe action (1.0 for full length, 2.0 for half the way, 0.5 for twice
     *                 the distance)
     * @throws SwipeException if the swipe action does not cause a change in the image
     */
    public static void swipeVertically(final String locator, final float reduction) {

        swipe(locator, null, reduction, false);
    }
}
