package works.lysenko.util.apis.common;

import org.openqa.selenium.WebElement;

/**
 * This interface defines methods for describing web elements.
 */
@SuppressWarnings({"InterfaceWithOnlyOneDirectInheritor", "BooleanParameter"})
public interface DescribesWebElements {

    /**
     * Describes the specified web element.
     *
     * @param element web element reference
     * @return description of the web element
     */
    String describe(WebElement element);

    /**
     * Describes the specified web element with optional geometry details.
     *
     * @param element  web element reference
     * @param geometry whether to include geometry information
     * @return description of the web element
     */
    String describe(WebElement element, boolean geometry);
}
