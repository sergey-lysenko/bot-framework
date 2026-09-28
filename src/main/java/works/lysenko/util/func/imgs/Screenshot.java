package works.lysenko.util.func.imgs;

import org.openqa.selenium.OutputType;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebElement;
import works.lysenko.util.data.records.ScreenshotSettings;
import works.lysenko.util.prop.grid.Marker;
import works.lysenko.util.spec.Layout;
import works.lysenko.util.spec.Level;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;

import static java.util.Objects.isNull;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static works.lysenko.Base.*;
import static works.lysenko.base.core.Routines.in;
import static works.lysenko.util.chrs.__.NO;
import static works.lysenko.util.chrs.___.PNG;
import static works.lysenko.util.chrs.___.XML;
import static works.lysenko.util.chrs.____.FULL;
import static works.lysenko.util.chrs.____.HTML;
import static works.lysenko.util.chrs.____.NAME;
import static works.lysenko.util.data.enums.Ansi.gray;
import static works.lysenko.util.data.enums.Platform.ANDROID;
import static works.lysenko.util.data.strs.Bind.b;
import static works.lysenko.util.data.strs.Case.c;
import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.data.strs.Vars.a;
import static works.lysenko.util.data.strs.Wrap.q;
import static works.lysenko.util.func.core.Assertions.fail;
import static works.lysenko.util.func.imgs.BufferedImages.getCropped;
import static works.lysenko.util.func.imgs.Painter.drawArea;
import static works.lysenko.util.func.type.Objects.isNotNull;
import static works.lysenko.util.lang.D.DUE_TO;
import static works.lysenko.util.lang.G.GETTING_FULLSCREEN_IMAGE;
import static works.lysenko.util.lang.G.GETTING_PARTIAL_SCREENSHOT_OF;
import static works.lysenko.util.lang.M.MAKING_SCREENSHOT;
import static works.lysenko.util.lang.S.SNAPSHOT_OF_PAGE_CODE;
import static works.lysenko.util.lang.U.*;
import static works.lysenko.util.lang.W.WRITING_CROPPED_IMAGE;
import static works.lysenko.util.lang.W.WRITING_FULLSCREEN_IMAGE;
import static works.lysenko.util.lang.W.WRITING_FULL_SCREENSHOT;
import static works.lysenko.util.lang.word.H.HEIGHT;
import static works.lysenko.util.lang.word.M.MAKING;
import static works.lysenko.util.lang.word.S.SCREENSHOT;
import static works.lysenko.util.lang.word.W.WIDTH;
import static works.lysenko.util.spec.Layout.Templates.RUN_ROOT_;
import static works.lysenko.util.spec.Layout.Templates.SCREENSHOT_;
import static works.lysenko.util.spec.Symbols.*;

/**
 * By default, getBufferedImage() methods also writing screenshots, which can potentially create considerable overhead
 */
@SuppressWarnings({"unused", "SameParameterValue", "ConfusingArgumentToVarargsMethod", "CallToPrintStackTrace"})
public record Screenshot() {

    /**
     * Retrieves a fullscreen screenshot as a BufferedImage.
     *
     * @return The captured fullscreen screenshot.
     * @throws RuntimeException If there is an IOException while reading the screenshot.
     */
    @SuppressWarnings({"ThrowInsideCatchBlockWhichIgnoresCaughtException", "MethodWithMultipleReturnPoints"})
    private static BufferedImage getFullscreenBufferedImage() {

        final TakesScreenshot webDriver = (TakesScreenshot) exec.wd();

        if (isNull(webDriver)) {
            fail(UNABLE_TO_READ___);
            return null;
        }
        logTrace(GETTING_FULLSCREEN_IMAGE); // expensive operation
        try {
            return ImageIO.read(new ByteArrayInputStream(webDriver.getScreenshotAs(OutputType.BYTES)));
        } catch (final org.openqa.selenium.WebDriverException e) {
            fail(b(UNABLE_TO_READ___, DUE_TO, q(e.getMessage())));
        } catch (final IOException e) {
            throw new IllegalStateException(b(UNABLE_TO_READ_SCREENSHOT___, q(e.getMessage())));
        }
        return null;
    }

    /**
     * @param template of path
     * @param root     place screenshot in root folder
     * @param v        values for filename
     * @return path of screenshot
     */
    public static String getSnapshotPath(final String template, final boolean root, final String... v) {

        return Layout.Files.name((root ? RUN_ROOT_ : template), v);
    }

    /**
     * Returns the window size of the WebDriver instance.
     *
     * @return The window size as a Dimension object.
     */
    private static org.openqa.selenium.Dimension getWindowSize() {

        return Objects.requireNonNull(exec.wd()).manage().window().getSize();
    }

    /**
     * @param root place snapshot in root folder
     * @param name of snapshot
     */
    @SuppressWarnings("ThrowInsideCatchBlockWhichIgnoresCaughtException")
    private static void makeCodeSnapshot(final boolean root, final String name) {

        log(Level.none, gray(b(s(RHT_ARR), c(MAKING), q(name), SNAPSHOT_OF_PAGE_CODE)), true);
        final String ext = in(ANDROID) ? XML : HTML;
        final Path path = Path.of(getSnapshotPath(SCREENSHOT_, root, name, ext));
        // Writes a page source to file; throws on failure
        try {
            if (isNotNull(exec.wd()))
                java.nio.file.Files.writeString(path, Objects.requireNonNull(exec.wd()).getPageSource());
        } catch (final IOException e) {
            throw new IllegalArgumentException(b(UNABLE_TO_WRITE_CODE___, q(name)));
        }
    }

    /**
     * Takes a screenshot of the screen and generates a code snapshot of the page code.
     *
     * @param name Parts of the screenshot file name. Pass an empty string or null if no name parts are needed.
     * @return The captured screenshot image.
     */
    public static BufferedImage makeScreenAndCodeSnapshot(final String name) {

        return makeScreenAndCodeSnapshot(false, name);
    }

    /**
     * Takes a screenshot of the screen and generates a code snapshot of the page code.
     *
     * @param root True if the screenshot file should be placed in the root directory. False otherwise.
     * @param name Parts of the screenshot file name. Pass an empty string or null if no name parts are needed.
     * @return The captured screenshot image.
     */
    public static BufferedImage makeScreenAndCodeSnapshot(final boolean root, final String name) {

        final BufferedImage bi = makeScreenshot(null, root, null, true, name);
        makeCodeSnapshot(root, name);
        return bi;
    }

    /**
     * @param name Parts of the screenshot file name. Pass an empty array or null if no name parts are needed.
     * @return The captured screenshot image.
     */
    public static BufferedImage makeScreenshot(final String... name) {

        final String[] names = noEmpty(name);
        return makeScreenshot(null, false, names);
    }

    /**
     * @param element The web element to capture a screenshot of. Pass null to capture a screenshot of the whole page.
     * @param name    Parts of the screenshot file name. Pass an empty array or null if no name parts are needed.
     * @return The captured screenshot image.
     */
    public static BufferedImage makeScreenshot(final WebElement element, final String... name) {

        final String[] names = noEmpty(name);
        return makeScreenshot(element, false, name);
    }

    /**
     * @param element The web element to capture a screenshot of. Pass null to capture a screenshot of the whole page.
     * @param root    True if the screenshot file should be placed in the root directory. False otherwise.
     * @param name    Parts of the screenshot file name. Pass an empty array or null if no name parts are needed.
     * @return The captured screenshot image.
     */
    private static BufferedImage makeScreenshot(final WebElement element, final boolean root, final String... name) {

        final String[] names = noEmpty(name);
        return makeScreenshot(element, root, true, name);
    }

    /**
     * @param element The web element to capture a screenshot of. Pass null to capture a screenshot of the whole page.
     * @param root    True if the screenshot file should be placed in the root directory. False otherwise.
     * @param write   True if the screenshot should be written to a file. False otherwise.
     * @param name    Parts of the screenshot file name. Pass an empty array or null if no name parts are needed.
     * @return The captured screenshot image.
     */
    private static BufferedImage makeScreenshot(final WebElement element, final boolean root, final boolean write,
                                                final String... name) {

        final String[] names = noEmpty(name);
        return makeScreenshot(element, root, null, write, name);
    }


    /**
     * Captures a screenshot of a webpage or a specific web element and returns it as a BufferedImage.
     *
     * @param element    The web element to capture a screenshot of. Pass null to capture a screenshot of the whole page.
     * @param root       True if the screenshot file should be placed in the root directory. False otherwise.
     * @param fullscreen A pre-captured fullscreen screenshot image. Pass null if not available.
     * @param write      True if the screenshot should be written to a file. False otherwise.
     * @param name       Parts of the screenshot file name. Pass an empty array or null if no name parts are needed.
     * @return The captured screenshot image.
     */
    public static BufferedImage makeScreenshot(final WebElement element, final boolean root, final BufferedImage fullscreen,
                                               final boolean write, final String... name) {

        return makeScreenshot(element, false, root, fullscreen, write, name);
    }

    /**
     * Captures a screenshot of a webpage or a specific web element and returns it as a BufferedImage.
     *
     * @param element    The web element to capture a screenshot of. Pass null to capture a screenshot of the whole page.
     * @param silent     If true, suppresses additional logging during the screenshot process. False otherwise.
     * @param root       True if the screenshot file should be placed in the root directory. False otherwise.
     * @param fullscreen A pre-captured fullscreen screenshot image. Pass null if not available.
     * @param write      True if the screenshot should be written to a file. False otherwise.
     * @param name       Parts of the screenshot file name. Pass an empty array or null if no name parts are needed.
     * @return The captured screenshot image as a BufferedImage.
     */
    @SuppressWarnings("MethodWithMultipleReturnPoints")
    public static BufferedImage makeScreenshot(final WebElement element, final boolean silent, final boolean root,
                                               final BufferedImage fullscreen, final boolean write, final String... name) {

        if (!silent) logMakingScreenshot(name);
        final BufferedImage img;
        if (isNotNull(fullscreen)) img = fullscreen;
        else // There are no alternatives - due to ongoing Appium issue it is only possible to make full screenshots
            img = getFullscreenBufferedImage();
        // Then, there are two options - do we need a full screenshot or partial one
        if (isNotNull(element))
            return writePartialScreenshot(img, new ScreenshotSettings(silent, (null == fullscreen), write, element, root),
                    name);
        else { // fullscreen
            if (write) return writeScreenshot(img, root, name);
            else return img;
        }
    }

    /**
     * Logs the process of making a screenshot with optional naming components.
     *
     * @param name An array of strings representing parts of the screenshot file name.
     *             If null or empty, the log will indicate a generic "making a screenshot" action.
     */
    private static void logMakingScreenshot(final String[] name) {

        // Logs screenshot creation with optional filename
        if (isNull(name)) log(Level.none, gray(b(s(DWN_ARR), MAKING_SCREENSHOT)), true);
        // Logs screenshot creation with optional filename components
        else log(Level.none, gray(b(false, s(DWN_ARR), c(MAKING), (0 == name.length || name[0].isEmpty()) ? EMPTY :
                q(s(name).replace(File.separator, s(_DASH_))), SCREENSHOT)), true);
    }

    private static String[] noEmpty(final String[] name) {

        String[] names = name;
        if (0 == names.length) {
            names = new String[1];
            names[0] = s(NO, NAME);
        }
        return names;
    }

    private static void storeCropped(final RenderedImage prune, final String croppedPath) throws IOException {

        logTrace(WRITING_CROPPED_IMAGE);
        ImageIO.write(prune, PNG, new File(croppedPath));
    }

    private static void storeFullscreen(final RenderedImage image, final String fullPath) throws IOException {

        logTrace(WRITING_FULLSCREEN_IMAGE);
        ImageIO.write(image, PNG, new File(fullPath));
    }

    /**
     * Writes a partial screenshot to an image file.
     *
     * @param image    The screenshot image to be cropped and written.
     * @param settings The screenshot settings including element to capture and file storage options.
     * @param name     Parts of the screenshot file name.
     * @return The cropped screenshot image.
     */
    @SuppressWarnings({"AssignmentToMethodParameter", "DataFlowIssue", "MethodWithMultipleReturnPoints"})
    private static BufferedImage writePartialScreenshot(BufferedImage image, final ScreenshotSettings settings,
                                                        final String... name) {

        final String fullPath = (null == name) ? null : getSnapshotPath(SCREENSHOT_, settings.root(), s(name), s(FULL, _DOT_
                , PNG));
        final String croppedPath = getSnapshotPath(SCREENSHOT_, settings.root(), (null == name) ? null : s(name), PNG);
        if (!settings.silent()) logDebug(b(GETTING_PARTIAL_SCREENSHOT_OF, q(exec.describe(settings.element()))));
        final Rectangle rect = settings.element().getRect();
        // Logs dimensions of element rectangle
        logTrace(b(a(s(X), rect.x, _COMMA_), a(s(Y), rect.y, _COMMA_), a(s(WIDTH), rect.width, _COMMA_), a(s(HEIGHT),
                rect.height)));
        // Stores screenshots; returns cropped image; handles exceptions
        try {
            final BufferedImage prune = getCropped(image, rect, settings.silent());
            image = drawArea(image, Marker.colour, rect);
            if (settings.storeFullscreen() && (isNotNull(fullPath))) storeFullscreen(image, fullPath);
            if (settings.storeCropped()) storeCropped(prune, croppedPath);
            return prune; // image returned in any case
        } catch (final IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Writes a screenshot to an image file.
     *
     * @param image The screenshot image to be written.
     * @param root  Flag indicating whether the file should be placed in the root directory.
     * @param name  Parts of the screenshot file name.
     * @return The same screenshot image that was passed in.
     * @throws IllegalArgumentException If there is an error writing the screenshot image.
     */
    @SuppressWarnings({"ThrowInsideCatchBlockWhichIgnoresCaughtException", "MethodWithMultipleReturnPoints"})
    public static BufferedImage writeScreenshot(final BufferedImage image, final Boolean root, final String... name) { //

        if (isNull(image)) return null;
        final String s = (null == name) ? EMPTY : s(name);
        // Writes image to file; throws on writing error
        try {
            final String path = getSnapshotPath(SCREENSHOT_, root, s, PNG);
            logDebug(b(WRITING_FULL_SCREENSHOT, q(path)));
            ImageIO.write(image, PNG, new File(path));
            return image;
        } catch (final IOException e) {
            throw new IllegalArgumentException(b(UNABLE_TO_WRITE_SCREENSHOT, q(s)));
        }
    }
}
