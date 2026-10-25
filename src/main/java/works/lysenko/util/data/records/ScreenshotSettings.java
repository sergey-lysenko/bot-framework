package works.lysenko.util.data.records;

import org.openqa.selenium.WebElement;

/**
 * Represents the settings for capturing a partial screenshot.
 *
 * @param silent          whether screenshot operations should run silently without logging
 * @param storeFullscreen whether to store the full screen image
 * @param storeCropped    whether to store the cropped element image
 * @param element         the web element associated with the screenshot
 * @param root            flag indicating whether this screenshot is taken from the root context
 */
public record ScreenshotSettings(boolean silent, boolean storeFullscreen, boolean storeCropped, WebElement element, Boolean root) {}
