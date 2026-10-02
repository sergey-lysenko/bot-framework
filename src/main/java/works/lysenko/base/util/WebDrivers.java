package works.lysenko.base.util;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.safari.SafariDriver;
import org.openqa.selenium.safari.SafariOptions;
import works.lysenko.util.apis.test._Test;
import works.lysenko.util.data.enums.Platform;

import static works.lysenko.Base.parameters;
import static works.lysenko.util.data.enums.ExitCode.UNDEFINED_BROWSER;
import static works.lysenko.util.func.type.Objects.isNotNull;

/**
 * @author Sergii Lysenko
 */
@SuppressWarnings({"UtilityClass", "FinalClass", "SwitchStatement", "ClassWithoutLogger", "PublicMethodWithoutLogging",
        "ImplicitCallToSuper",
        "ClassWithTooManyTransitiveDependents", "UtilityClassCanBeEnum", "ClassUnconnectedToPackage",
        "ClassWithTooManyTransitiveDependencies",
        "CyclicClassDependency"})
public final class WebDrivers {

    private WebDrivers() {

    }

    /**
     * @return default Chrome {@link org.openqa.selenium.WebDriver} instance
     */
    @SuppressWarnings({"unused", "UnqualifiedStaticUsage"})
    public static WebDriver get() {

        return get(Platform.CHROME, false);
    }

    /**
     * @param p {@link Platform} to create
     *          {@link org.openqa.selenium.WebDriver} for
     * @return correspondent {@link org.openqa.selenium.WebDriver} object
     */
    @SuppressWarnings({"LocalCanBeFinal", "UnqualifiedStaticUsage"})
    public static WebDriver get(Platform p) {

        return get(p, false);
    }

    /**
     * @param p        {@link Platform} to create
     *                 {@link org.openqa.selenium.WebDriver} for
     * @param maximize the browser window of not
     * @return correspondent {@link org.openqa.selenium.WebDriver} object
     */
    @SuppressWarnings({"BooleanParameter", "ChainedMethodCall", "LocalCanBeFinal", "NestedMethodCall",
            "UnqualifiedStaticUsage", "SameParameterValue"})
    public static WebDriver get(Platform p, boolean maximize) {

        final boolean headless = isNotNull(parameters) && parameters.isHeadless();
        return get(p, maximize, headless);
    }

    /**
     * @param p        {@link Platform} to create
     *                 {@link org.openqa.selenium.WebDriver} for
     * @param maximize the browser window of not
     * @param headless whether to run browser in headless mode
     * @return correspondent {@link org.openqa.selenium.WebDriver} object
     */
    @SuppressWarnings({"BooleanParameter", "ChainedMethodCall", "LocalCanBeFinal", "NestedMethodCall",
            "UnqualifiedStaticUsage", "SameParameterValue"})
    public static WebDriver get(Platform p, boolean maximize, boolean headless) {

        WebDriver driver = null;
        switch (p) {
            case FIREFOX -> {
                WebDriverManager.firefoxdriver().setup();
                driver = new FirefoxDriver(getFireFoxOptions(headless));
            }
            case EDGE -> {
                WebDriverManager.edgedriver().setup();
                driver = new EdgeDriver(getEdgeOptions(headless));
            }
            case SAFARI -> {
                WebDriverManager.safaridriver().setup();
                driver = new SafariDriver(getSafariOptions());
            }
            case CHROME -> {
                WebDriverManager.chromedriver().setup();
                driver = new ChromeDriver(getChromeOptions(headless));
            }
            // d = WebDriverManager.chromedriver().browserInDockerAndroid().create();
            default -> _Test.processCode(UNDEFINED_BROWSER);
        }
        if (maximize && !headless)
            if (isNotNull(driver))
                driver.manage().window().maximize();
        return driver;
    }

    @SuppressWarnings("LocalCanBeFinal")
    private static ChromeOptions getChromeOptions(final boolean headless) {

        ChromeOptions options = new ChromeOptions();
        if (headless) {
            options.addArguments("--headless=new"); //NON-NLS
        }
        options.addArguments("--window-size=1920,1080"); //NON-NLS
        options.addArguments("--lang=en-GB"); //NON-NLS
        options.addArguments("--disable-gpu"); //NON-NLS
        options.addArguments("--no-sandbox"); //NON-NLS
        options.addArguments("--disable-dev-shm-usage"); //NON-NLS
        options.addArguments("--disable-setuid-sandbox"); //NON-NLS
        options.addArguments("--disable-infobars"); //NON-NLS
        options.addArguments("--enable-logging");  // NON-NLS
        options.addArguments("--v=1"); //NON-NLS
        return options;
    }

    @SuppressWarnings("unused")
    private static ChromeOptions getChromeOptions() {

        return getChromeOptions(false);
    }

    private static EdgeOptions getEdgeOptions(final boolean headless) {

        EdgeOptions options = new EdgeOptions();
        if (headless) {
            options.addArguments("--headless=new"); //NON-NLS
            options.addArguments("--window-size=1920,1080"); //NON-NLS
        }
        return options;
    }

    @SuppressWarnings("unused")
    private static EdgeOptions getEdgeOptions() {

        return getEdgeOptions(false);
    }

    @SuppressWarnings("LocalCanBeFinal")
    private static FirefoxOptions getFireFoxOptions(final boolean headless) {

        FirefoxOptions options = new FirefoxOptions();
        if (headless) {
            options.addArguments("-headless"); //NON-NLS
            options.addArguments("--width=1920"); //NON-NLS
            options.addArguments("--height=1080"); //NON-NLS
        }
        options.setCapability("devtools.console.stdout.content", true); //NON-NLS
        return options;
    }

    @SuppressWarnings("unused")
    private static FirefoxOptions getFireFoxOptions() {

        return getFireFoxOptions(false);
    }

    private static SafariOptions getSafariOptions() {

        return new SafariOptions();
    }

}