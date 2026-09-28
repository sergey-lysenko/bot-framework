package works.lysenko.util.func.ui.scroll;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Interactive;
import org.openqa.selenium.interactions.PointerInput;
import org.openqa.selenium.interactions.Sequence;
import works.lysenko.util.apis.exception.unchecked.SwipeException;
import works.lysenko.util.apis.grid.t._RelativePosition;
import works.lysenko.util.data.records.Location;
import works.lysenko.util.prop.core.Screenshot;
import works.lysenko.util.prop.core.Size;
import works.lysenko.util.prop.core.Swipe;

import java.awt.Point;
import java.awt.image.BufferedImage;
import java.time.Duration;
import java.util.List;

import static java.lang.StrictMath.round;
import static java.util.Objects.isNull;
import static works.lysenko.Base.exec;
import static works.lysenko.Base.logDebug;
import static works.lysenko.util.chrs.__.*;
import static works.lysenko.util.chrs.___.NOT;
import static works.lysenko.util.chrs.____.DONE;
import static works.lysenko.util.chrs.____.FROM;
import static works.lysenko.util.data.strs.Bind.b;
import static works.lysenko.util.data.strs.Case.c;
import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.func.Imgs.areIdentical;
import static works.lysenko.util.func.imgs.BufferedImages.getBufferedImage;
import static works.lysenko.util.func.imgs.Painter.drawLineOn;
import static works.lysenko.util.func.imgs.Painter.drawPointOn;
import static works.lysenko.util.func.imgs.Screenshot.writeScreenshot;
import static works.lysenko.util.lang.word.A.AFTER;
import static works.lysenko.util.lang.word.C.*;
import static works.lysenko.util.lang.word.D.DID;
import static works.lysenko.util.lang.word.D.DURING;
import static works.lysenko.util.lang.word.E.ELEMENT;
import static works.lysenko.util.lang.word.F.FINGER;
import static works.lysenko.util.lang.word.I.IMAGE;
import static works.lysenko.util.lang.word.I.INITIAL;
import static works.lysenko.util.lang.word.S.SWIPE;
import static works.lysenko.util.lang.word.S.SWIPING;
import static works.lysenko.util.lang.word.T.TARGET;
import static works.lysenko.util.lang.word.T.TOUCHING;
import static works.lysenko.util.spec.Symbols.X;
import static works.lysenko.util.spec.Symbols.Y;
import static works.lysenko.util.spec.Symbols._COLON_;

/**
 * A record class representing data required for swipe actions performed on web elements.
 *
 * @param scroller The WebElement representing the element to be scrolled or swiped through.
 * @param height   The height of the scroller element.
 * @param width    The width of the scroller element.
 * @param finger   The PointerInput representing a simulated finger for touch actions.
 * @param q        The sequence of PointerInput actions for performing the swipe.
 */
@SuppressWarnings({"StaticMethodOnlyUsedInOneClass", "StandardVariableNames"})
public record Data(WebElement scroller, int height, int width, PointerInput finger, Sequence q) {

    private static final String S1 = b(c(AFTER), SWIPE);
    private static final String S2 = b(c(SWIPE), DID, NOT, CAUSE, CHANGE, OF, IMAGE);
    private static final String S3 = b(c(SWIPE), FROM);
    private static final String S4 = b(c(TOUCHING), AT, INITIAL, s(COORDINATES, _COLON_));
    private static final String S5 = b(c(CURRENT), s(ELEMENT, _COLON_));
    private static final String S6 = b(c(SWIPING), TO, TARGET, s(COORDINATES, _COLON_));
    private static final String S7 = b(c(SWIPE), DONE);

    /**
     * Performs any necessary actions after taking a screenshot, such as comparing it with a previous screenshot.
     *
     * @param before The before screenshot image used for comparison.
     */
    public static void getAfterScreenshot(final BufferedImage before) {

        final BufferedImage after = getBufferedImage();
        if (Screenshot.swipes) writeScreenshot(after, false, S1);
        if (areIdentical(before, after)) throw new SwipeException(S2);
    }

    /**
     * Retrieves the before screenshot image with added markers for swipe action.
     *
     * @param s        The starting point of the swipe action.
     * @param t        The target point of the swipe action.
     * @param duration The duration of the swipe action.
     * @return The before screenshot image with added markers.
     */
    public static BufferedImage getBeforeScreenshot(final Point s, final Point t, final int duration) {

        final BufferedImage image = getBufferedImage();
        if (Screenshot.swipes) {
            drawPointOn(image, Swipe.start, new Location(s.x, s.y), Size.longClicksMarker);
            drawPointOn(image, Swipe.stop, new Location(t.x, t.y), Size.longClicksMarker);
            drawLineOn(image, Swipe.line, new Location(s.x, s.y), new Location(t.x, t.y));
            writeScreenshot(image, false, b(S3, s(X, s.x), s(Y, s.y), TO, s(X, t.x), s(Y, t.y), s(duration, MS)));
        }
        return image;
    }

    /**
     * Retrieves the swipe data for performing a swipe action on an element.
     *
     * @param locator The locator of the element to perform the swipe action on.
     * @return The swipe data containing the necessary information for the swipe action.
     */
    public static Data getData(final String locator) {

        final WebElement scroller = exec.find(locator);
        final int height = scroller.getRect().height;
        final int width = scroller.getRect().width;
        final PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, FINGER);
        final Sequence swipe = new Sequence(finger, 1);
        logDebug(b(S5, exec.describe(scroller)));
        return new Data(scroller, height, width, finger, swipe);
    }

    /**
     * Retrieves the starting point for an action, given the data and relative position.
     *
     * @param d                the DataStorage object containing necessary information
     * @param relativePosition the relative position of the starting point
     * @return the starting Point for the action
     */
    public static Point getStartingPoint(final Data d, final _RelativePosition relativePosition) {

        final int x = Math.toIntExact(d.scroller().getLocation().x + round(d.width() * relativePosition.h()));
        final int y = Math.toIntExact(d.scroller().getLocation().y + round(d.height() * relativePosition.v()));
        logDebug(b(S4, s(x), s(y)));
        d.q().addAction(d.finger().createPointerMove(Duration.ofMillis(0L),
                PointerInput.Origin.viewport(), x, y));
        d.q().addAction(d.finger().createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
        return new Point(x, y);
    }

    /**
     * Calculates the target point for a swipe action.
     *
     * @param reduction    the reduction factor for each swipe action (1.0 for full length, 2.0 for half the way, 0.5 for
     *                     twice the distance)
     * @param horizontally set to true to perform a horizontal swipe, false for vertical swipe
     * @param s            the starting point of the swipe action
     * @param d            the DataStorage object containing necessary information
     * @param duration     the duration of the swipe action
     * @return the target point for the swipe action
     */
    public static Point getTargetPoint(final float reduction, final boolean horizontally, final Point s, final Data d,
                                       final int duration) {

        final int x = horizontally ? s.x + round(d.width() / reduction) : s.x;
        final int y = horizontally ? s.y : s.y + round(d.height() / reduction);
        logDebug(b(S6, s(x), s(y), DURING, s(duration, MS)));
        return new Point(x, y);
    }

    /**
     * Performs a swipe action on the specified element with the given duration, starting point, and target point.
     *
     * @param d        The DataStorage object containing necessary information for the swipe action.
     * @param duration The duration of the swipe action in milliseconds.
     * @param s        The starting point of the swipe action.
     * @param t        The target point of the swipe action.
     */
    @SuppressWarnings("MethodWithMultipleReturnPoints")
    public static void performSwipingAction(final Data d, final int duration, final Point s, final Point t) {

        final WebDriver wd = exec.wd();
        if (isNull(wd)) return;
        d.q().addAction(d.finger().createPointerMove(Duration.ofMillis(duration),
                PointerInput.Origin.viewport(), s.x, t.y));
        d.q().addAction(d.finger().createPointerUp(PointerInput.MouseButton.LEFT.asArg()));
        ((Interactive) wd).perform(List.of(d.q()));
        logDebug(S7);
        exec.sleepShort();
    }
}
