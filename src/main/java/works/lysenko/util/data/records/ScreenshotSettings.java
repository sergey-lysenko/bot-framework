package works.lysenko.util.data.records;

import org.openqa.selenium.WebElement;

/**
 * Represents the settings for capturing a partial screenshot.
 */
public record ScreenshotSettings(boolean silent, boolean storeFullscreen, boolean storeCropped, WebElement element, Boolean root) {}
