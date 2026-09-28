package works.lysenko.util.apis.common;

import org.openqa.selenium.WebElement;

/**
 * This interface defines methods for describing web elements.
 */
@SuppressWarnings({"InterfaceWithOnlyOneDirectInheritor", "BooleanParameter"})
public interface DescribesWebElements {

    /**
     * @param element reference
     * @return description
     */
    String describe(WebElement element);

    /**
     * @param element  reference
     * @param geometry whether to include geometry information
     * @return description
     */
    String describe(WebElement element, boolean geometry);
}
