package works.lysenko.util.grid;

import org.openqa.selenium.Rectangle;
import org.openqa.selenium.WebElement;
import works.lysenko.util.apis.grid._GridProvider;
import works.lysenko.util.data.enums.Severity;
import works.lysenko.util.func.imgs.BufferedImages;
import works.lysenko.util.func.imgs.Grid;
import works.lysenko.util.func.imgs.OperationMarkers;
import works.lysenko.util.func.imgs.Screenshot;
import works.lysenko.util.grid.validation.BackgroundValidator;
import works.lysenko.util.grid.validation.Check;

import java.awt.Point;
import java.awt.image.BufferedImage;
import java.util.List;

/**
 * Default {@link _GridProvider} implementation backed by {@link Screenshot}, {@link OperationMarkers},
 * {@link Grid}, {@link Check}, and {@link BackgroundValidator}.
 */
public final class DefaultGridProvider implements _GridProvider {

    @Override
    public List<String> getAvailableMarkerModes() {
        return OperationMarkers.availableModes();
    }

    @Override
    public BufferedImage captureBufferedImage() {
        return BufferedImages.getBufferedImage();
    }

    @Override
    public BufferedImage captureScreenshot(final String... name) {
        return Screenshot.makeScreenshot(name);
    }

    @Override
    public BufferedImage captureElementScreenshot(final WebElement element, final String... name) {
        return Screenshot.makeScreenshot(element, name);
    }

    @Override
    public BufferedImage storeScreenshot(final BufferedImage image, final Boolean root, final String... name) {
        return Screenshot.writeScreenshot(image, root, name);
    }

    @Override
    public BufferedImage annotateElement(final BufferedImage image, final Rectangle rect,
                                         final String label, final String action) {
        return OperationMarkers.markElement(image, rect, label, action);
    }

    @Override
    public BufferedImage annotatePoint(final BufferedImage image, final Point point, final String action) {
        return OperationMarkers.markPoint(image, point, action);
    }

    @Override
    public BufferedImage annotateSwipe(final BufferedImage image, final Point start, final Point end) {
        return OperationMarkers.markSwipe(image, start, end);
    }

    @Override
    public boolean validateImage(final BufferedImage image, final String name) {
        return Grid.test(Check.image(image, name)).isPassed();
    }

    @Override
    public void createBackgroundValidator(final String locator, final String gridName,
                                          final BufferedImage inImage, final Severity failureSeverity) {
        BackgroundValidator.bv(locator, gridName, null, inImage, failureSeverity);
    }
}
