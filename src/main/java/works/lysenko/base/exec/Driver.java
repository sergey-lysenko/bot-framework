package works.lysenko.base.exec;

import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.service.local.AppiumDriverLocalService;
import io.appium.java_client.service.local.AppiumServiceBuilder;
import org.openqa.selenium.Point;
import org.openqa.selenium.SessionNotCreatedException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.support.ui.WebDriverWait;
import works.lysenko.base.core.Routines;
import works.lysenko.base.util.WebDrivers;
import works.lysenko.util.apis.execution._Driver;
import works.lysenko.util.data.enums.Platform;
import works.lysenko.util.prop.core.Waits;
import works.lysenko.util.prop.logs.Debug;
import works.lysenko.util.spec.Level;

import java.io.File;
import java.io.IOException;
import java.time.Duration;

import static java.util.Objects.isNull;
import static works.lysenko.Base.log;
import static works.lysenko.base.util.Platforms.__BASE_PATH;
import static works.lysenko.util.chrs.___.*;
import static works.lysenko.util.chrs.____.FROM;
import static works.lysenko.util.chrs.____.WARN;
import static works.lysenko.util.data.strs.Bind.b;
import static works.lysenko.util.data.strs.Case.c;
import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.data.strs.Wrap.q;
import static works.lysenko.util.func.core.Assertions.fail;
import static works.lysenko.util.func.type.Objects.isNotNull;
import static works.lysenko.util.lang.A.*;
import static works.lysenko.util.lang.M.MAIN_ACTIVITY;
import static works.lysenko.util.lang.U.*;
import static works.lysenko.util.lang.W.WD_HUB;
import static works.lysenko.util.lang.word.A.ADDITIONAL;
import static works.lysenko.util.lang.word.A.ANDROID;
import static works.lysenko.util.lang.word.A.APPIUM;
import static works.lysenko.util.lang.word.B.BROWSER;
import static works.lysenko.util.lang.word.C.COMPONENTS;
import static works.lysenko.util.lang.word.C.CONFIGURING;
import static works.lysenko.util.lang.word.C.CREATING;
import static works.lysenko.util.lang.word.D.DRIVER;
import static works.lysenko.util.lang.word.I.INITIALIZING;
import static works.lysenko.util.lang.word.I.INSTANTIATE;
import static works.lysenko.util.lang.word.L.LEVEL;
import static works.lysenko.util.lang.word.R.REARRANGING;
import static works.lysenko.util.lang.word.S.SERVICE;
import static works.lysenko.util.lang.word.S.STARTING;
import static works.lysenko.util.lang.word.W.WINDOW;
import static works.lysenko.util.spec.PropEnum._APP;
import static works.lysenko.util.spec.PropEnum._BUNDLE_ID;
import static works.lysenko.util.spec.PropEnum._UD_ID;
import static works.lysenko.util.spec.Symbols._DASH_;
import static works.lysenko.util.spec.Symbols._DOT_;

/**
 * The Driver class provides functionality to interact with WebDriver and AppiumDriver.
 * It includes methods to manage and configure drivers, services, and their associated capabilities.
 */
public class Driver implements _Driver {

    private WebDriverWait wdw = null;
    private WebDriver wd = null;
    private AppiumDriverLocalService service = null;

    public final AppiumDriverLocalService service() {

        return service;
    }

    public final WebDriver wd() {

        return wd;
    }

    public final WebDriverWait wdw() {

        return wdw;
    }

    public final void service(final AppiumDriverLocalService service) {

        this.service = service;
    }

    public final void wd(final WebDriver wd) {

        this.wd = wd;
    }

    public final void wdw(final WebDriverWait wdw) {

        this.wdw = wdw;
    }

    public final boolean isOk() {

        log(Level.none, b(c(INITIALIZING), c(DRIVER), DOTS), true);
        final String platform = Routines.get_Platform();
        if (Platform.ANDROID == Platform.get(platform)) startAndroid();
        else startBrowser();
        log(Level.none, b(c(CREATING), ADDITIONAL, COMPONENTS, DOTS), true);
        createAdditional();
        return isNotNull(wd);
    }

    private void startAndroid() {

        createService();
        startService();
        startAndroidDriver(configureAndroidDriver());
    }

    /**
     * Starts the browser and maximises the window.
     */
    private void startBrowser() {

        log(Level.none, b(c(CREATING), s(c(WEB), c(DRIVER)), DOTS), true);
        wd = WebDrivers.get(Platform.get(Routines.get_Platform()), false);

        log(Level.none, b(c(REARRANGING), BROWSER, WINDOW, DOTS), false);
        wd.manage().window().setPosition(new Point(0, 0));
        wd.manage().window().maximize();
    }

    /**
     * Creates additional components for the execution.
     * These components include web driver parameters, web driver wait, and a set of not reproduced issues.
     */
    private void createAdditional() {

        if (isNotNull(wd)) {
            // Web driver parameters
            wd.manage().timeouts().implicitlyWait(Duration.ofMillis(Waits.implicit));
            wdw = new WebDriverWait(wd, Duration.ofSeconds(Waits.explicit));
        }
    }

    /**
     * Creates a service for executing automation.
     * This method initialises the AppiumDriverLocalService with necessary configurations.
     */
    private void createService() {

        log(Level.none, b(c(CREATING), c(APPIUM), c(SERVICE), DOTS), true);
        // Configures service with verbose or warning logging
        if (Debug.adebug)
            service =
                    new AppiumServiceBuilder().withArgument(() -> __BASE_PATH, WD_HUB).withLogFile(new File(APPIUM_LOG)).build();
        else
            service = new AppiumServiceBuilder().withArgument(() -> __BASE_PATH, WD_HUB).withArgument(() -> s(_DASH_, _DASH_
                    , LOG, _DASH_, LEVEL), WARN).withLogFile(new File(APPIUM_LOG)).build();

    }

    /**
     * Starts the service for executing automation.
     * This method initialises the AppiumDriverLocalService with necessary configurations.
     */
    @SuppressWarnings("resource")
    private void startService() {

        log(Level.none, b(c(STARTING), c(APPIUM), c(SERVICE), DOTS), true);
        service().start();
    }

    /**
     * Starts the Android driver and configures the desired capabilities.
     *
     * @param capabilities The desired capabilities for the Android driver.
     */
    @SuppressWarnings({"resource", "CallToPrintStackTrace"})

    private void startAndroidDriver(final DesiredCapabilities capabilities) {

        log(Level.none, b(c(CREATING), c(ANDROID), c(DRIVER), DOTS), true);
        // Instantiates Android driver; fails on session creation error
        try {
            wd = new AndroidDriver(service().getUrl(), capabilities);
        } catch (final SessionNotCreatedException e) {
            e.printStackTrace();
            fail(b(UNABLE_TO, INSTANTIATE, c(APPIUM), c(DRIVER)));
        }
    }

    /**
     * Configures the driver for executing the automation.
     *
     * @return The desired capabilities for the driver.
     * @throws IllegalArgumentException if unable to load the app from the specified file path.
     */
    @SuppressWarnings({"DataFlowIssue", "AccessOfSystemProperties", "ThrowInsideCatchBlockWhichIgnoresCaughtException"})
    private static DesiredCapabilities configureAndroidDriver() {

        log(Level.none, b(c(CONFIGURING), c(DRIVER), DOTS), true);

        File app = null;
        final String apk = _APP.get();

        if (!apk.isEmpty()) {
            final File appDir = new File(System.getProperty(USER_DIR));
            // Attempts to resolve an application path; throws on failure
            try {
                app = new File(appDir.getCanonicalPath(), _APP.get());
            } catch (final IOException e) {
                throw new IllegalArgumentException(b(UNABLE_TO_LOAD, APP, FROM, q(app.toString())));
            }
        }
        return getDesiredCapabilities(app);
    }

    /**
     * Retrieves and configures the desired capabilities for the Appium driver.
     * This method sets the automation name and invokes additional configuration
     * methods to define the application and device settings.
     *
     * @param app The File object representing the mobile application file (.apk or .ipa).
     *            If null, the application is configured using the bundle ID.
     * @return M DesiredCapabilities instance containing the configured capabilities
     * for the Appium driver.
     */
    private static DesiredCapabilities getDesiredCapabilities(final File app) {

        final DesiredCapabilities capabilities = new DesiredCapabilities();
        capabilities.setCapability(APPIUM_AUTOMATION_NAME, UI_AUTOMATOR_2);
        defineApplication(app, capabilities);
        defineDevice(capabilities);
        return capabilities;
    }

    /**
     * Configures the device-related capability for Appium by setting the UDID.
     * If a UDID is available, it sets the corresponding Appium capability.
     *
     * @param capabilities The DesiredCapabilities instance to configure device settings.
     */
    private static void defineDevice(final DesiredCapabilities capabilities) {

        final String udid = _UD_ID.get();
        if (!udid.isEmpty()) capabilities.setCapability(APPIUM_UDID, udid);
    }

    /**
     * Configures the application capability for automation.
     * If the `app` parameter is null, it uses the bundle ID to set the Appium package and activity capabilities.
     * If the `app` parameter is not null, it sets the capability using the absolute path of the provided app file.
     *
     * @param app          The File object representing the mobile application file (.apk or .ipa).
     *                     If null, the application is configured using the bundle ID.
     * @param capabilities The DesiredCapabilities object to which the application configuration will be set.
     * @throws IllegalArgumentException if the `app` parameter is null and no valid bundle ID is present.
     */
    private static void defineApplication(final File app, final DesiredCapabilities capabilities) {

        // Configures application based on file or bundle ID
        if (isNull(app)) {
            final String bid = _BUNDLE_ID.get();
            if (bid.isEmpty()) throw new IllegalArgumentException(b(UNABLE_TO_START_TEST___));
            capabilities.setCapability(APPIUM_APP_PACKAGE, bid);
            capabilities.setCapability(APPIUM_APP_ACTIVITY, s(bid, _DOT_, MAIN_ACTIVITY));
            capabilities.setCapability(APPIUM_NO_RESET, true);
        } else capabilities.setCapability(APP, app.getAbsolutePath());
    }
}
