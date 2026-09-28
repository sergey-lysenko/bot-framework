package works.lysenko.util.func.ui;

import org.apache.commons.math3.fraction.Fraction;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Interactive;
import org.openqa.selenium.interactions.PointerInput;
import org.openqa.selenium.interactions.Sequence;
import works.lysenko.util.data.records.Location;
import works.lysenko.util.prop.core.Screenshot;
import works.lysenko.util.prop.core.Size;
import works.lysenko.util.prop.grid.Marker;

import java.awt.image.BufferedImage;
import java.time.Duration;
import java.util.List;

import static org.apache.commons.math3.util.FastMath.round;
import static works.lysenko.Base.exec;
import static works.lysenko.Base.logDebug;
import static works.lysenko.util.chrs.__.AT;
import static works.lysenko.util.chrs.__.MS;
import static works.lysenko.util.chrs.____.DONE;
import static works.lysenko.util.chrs.____.LONG;
import static works.lysenko.util.chrs.____.WITH;
import static works.lysenko.util.data.strs.Bind.b;
import static works.lysenko.util.data.strs.Bind.d;
import static works.lysenko.util.data.strs.Case.c;
import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.data.strs.Vars.a;
import static works.lysenko.util.func.imgs.BufferedImages.getBufferedImage;
import static works.lysenko.util.func.imgs.Painter.drawPointOn;
import static works.lysenko.util.func.imgs.Screenshot.makeScreenshot;
import static works.lysenko.util.func.imgs.Screenshot.writeScreenshot;
import static works.lysenko.util.lang.word.A.AFTER;
import static works.lysenko.util.lang.word.B.BEFORE;
import static works.lysenko.util.lang.word.C.CLICK;
import static works.lysenko.util.lang.word.C.COORDINATES;
import static works.lysenko.util.lang.word.D.DIALOGUE;
import static works.lysenko.util.lang.word.D.DURATION;
import static works.lysenko.util.lang.word.F.FINGER;
import static works.lysenko.util.lang.word.H.HEIGHT;
import static works.lysenko.util.lang.word.P.PERFORMING;
import static works.lysenko.util.lang.word.W.WIDTH;
import static works.lysenko.util.spec.Symbols.X;
import static works.lysenko.util.spec.Symbols.Y;
import static works.lysenko.util.spec.Symbols._COMMA_;

/**
 * The Click class provides methods for performing click operations.
 */
@SuppressWarnings({"StaticMethodOnlyUsedInOneClass", "ClassIndependentOfModule", "unused"})
public record Click() {

    /**
     * Clicks at the specified coordinates on the screen.
     *
     * @param x the x-coordinate of the point to click
     * @param y the y-coordinate of the point to click
     */
    @SuppressWarnings("WeakerAccess")
    public static void clickAtCoordinates(final int x, final int y) {

        longClickAtCoordinates(0L, x, y);
    }

    /**
     * Clicks at the specified relative coordinates within the given WebElement.
     *
     * @param element   the WebElement to click on
     * @param relativeX the relative x-coordinate within the WebElement to click at (0.0 to 1.0)
     * @param relativeY the relative y-coordinate within the WebElement to click at (0.0 to 1.0)
     */
    @SuppressWarnings({"NumericCastThatLosesPrecision", "StaticMethodOnlyUsedInOneClass"})
    public static void clickAtRelativeCoordinates(final WebElement element, final Fraction relativeX,
                                                  final Fraction relativeY) {

        makeScreenshot(element, c(DIALOGUE));
        final int x1 = element.getRect().x;
        final int y1 = element.getRect().y;
        final org.openqa.selenium.Rectangle bounds = element.getRect();
        final double height = bounds.getHeight();
        final double width = bounds.getWidth();
        logDebug(b(a(HEIGHT, height, _COMMA_), a(WIDTH, width)));
        clickAtCoordinates((int) round(x1 + width * relativeX.doubleValue()),
                (int) round(y1 + height * relativeY.doubleValue()));
    }

    /**
     * Performs a long click at the specified coordinates on the screen.
     *
     * @param duration the duration of the long click in milliseconds
     * @param x1       the x-coordinate of the point to long click
     * @param y1       the y-coordinate of the point to long click
     */
    @SuppressWarnings({"WeakerAccess", "MethodWithMultipleReturnPoints"})
    public static void longClickAtCoordinates(final long duration, final int x1, final int y1) {

        final WebDriver wd = exec.wd();
        if (null == wd) return;
        final PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, FINGER);
        final Sequence click = new Sequence(finger, 1);
        click.addAction(finger.createPointerMove(Duration.ofMillis(0L), PointerInput.Origin.viewport(), x1, y1));
        click.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
        click.addAction(finger.createPointerMove(Duration.ofMillis(duration), PointerInput.Origin.viewport(), x1, y1));
        click.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));
        longClickScreenshot(BEFORE, duration, x1, y1);
        logDebug(b(b(c(PERFORMING), d(LONG, CLICK), AT, COORDINATES),
                s(X, x1), s(Y, y1), WITH, s(duration, MS), DURATION));
        ((Interactive) wd).perform(List.of(click));
        longClickScreenshot(AFTER, duration, x1, y1);
        logDebug(DONE);
    }

    /**
     * Takes a screenshot and marks a point on the image if a certain condition is met.
     *
     * @param name     the name of the screenshot
     * @param duration the duration of the long click in milliseconds
     * @param x1       the x-coordinate of the point to long click
     * @param y1       the y-coordinate of the point to long click
     */
    private static void longClickScreenshot(final String name, final long duration, final int x1, final int y1) {

        if (Screenshot.longClicks) {
            final BufferedImage image = getBufferedImage();
            drawPointOn(image, Marker.colour, new Location(x1, y1), Size.longClicksMarker);
            writeScreenshot(image, false, b(name, LONG, CLICK, AT, s(X, x1), s(Y, y1), s(duration, MS)));
        }
    }
}
