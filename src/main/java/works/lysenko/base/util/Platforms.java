package works.lysenko.base.util;

import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.service.local.AppiumDriverLocalService;
import io.appium.java_client.service.local.AppiumServiceBuilder;
import org.apache.commons.lang3.StringUtils;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.DesiredCapabilities;
import works.lysenko.util.data.enums.Platform;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import static works.lysenko.util.chrs.__.TO;
import static works.lysenko.util.chrs.___.APP;
import static works.lysenko.util.chrs.___.NEW;
import static works.lysenko.util.chrs.____.*;
import static works.lysenko.util.data.strs.Bind.b;
import static works.lysenko.util.data.strs.Bind.d;
import static works.lysenko.util.data.strs.Case.c;
import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.data.strs.Wrap.q;
import static works.lysenko.util.lang.A.APPIUM_AUTOMATION_NAME;
import static works.lysenko.util.lang.U.UI_AUTOMATOR_2;
import static works.lysenko.util.lang.U.UNABLE_TO_LOAD;
import static works.lysenko.util.lang.U.USER_DIR;
import static works.lysenko.util.lang.W.WD_HUB;
import static works.lysenko.util.lang.word.C.COMMAND;
import static works.lysenko.util.lang.word.D.DEFAULT;
import static works.lysenko.util.lang.word.P.PLATFORMS;
import static works.lysenko.util.lang.word.T.TIMEOUT;
import static works.lysenko.util.lang.word.U.UNABLE;
import static works.lysenko.util.lang.word.W.WRITE;
import static works.lysenko.util.prop.core.Start.forcedPlatform;
import static works.lysenko.util.prop.data.Delimeters.L1;
import static works.lysenko.util.spec.Layout.Files.APPIUM_LOG_;
import static works.lysenko.util.spec.Layout.Files.DEFAULT_ANDROID_APPLICATION_;
import static works.lysenko.util.spec.Layout.Files.PLATFORMS_;
import static works.lysenko.util.spec.Symbols._COMMA_;
import static works.lysenko.util.spec.Symbols._DASH_;
import static works.lysenko.util.spec.Symbols._DOT_;


/**
 * Platforms
 */
@SuppressWarnings({"ResultOfMethodCallIgnored", "OverlyBroadCatchBlock", "ProhibitedExceptionThrown",
        "ThrowInsideCatchBlockWhichIgnoresCaughtException", "AccessOfSystemProperties"})
public record Platforms() {

    /**
     * --base-path
     */
    public static final String __BASE_PATH = d(s(_DASH_), BASE, PATH);
    private static final int ENOUGH = 3000;

    /**
     * @return available platforms
     */
    public static List<Platform> available() {

        List<Platform> platforms = load();
        if (platforms.isEmpty()) {
            platforms = scan();
            save(platforms);
        }
        return platforms;
    }

    private static DesiredCapabilities getDesiredCapabilities(final File appDir) {

        final File app;
        try {
            app = new File(appDir.getCanonicalPath(), DEFAULT_ANDROID_APPLICATION_);
        } catch (final IOException e) {
            throw new RuntimeException(b(UNABLE_TO_LOAD, DEFAULT, APP, FROM, DEFAULT_ANDROID_APPLICATION_));
        }
        final DesiredCapabilities capabilities = new DesiredCapabilities();
        capabilities.setCapability(APPIUM_AUTOMATION_NAME, UI_AUTOMATOR_2);
        capabilities.setCapability(APP, app.getAbsolutePath());
        capabilities.setCapability(s(NEW, c(COMMAND), c(TIMEOUT)), ENOUGH);
        return capabilities;
    }

    private static List<Platform> load() {

        List<String> platformNames;
        try {
            platformNames = Arrays.asList((null == forcedPlatform) ?
                    Files.readString(Paths.get(PLATFORMS_))
                            .split(s(L1), -1) : forcedPlatform.split(s(L1), -1));
        } catch (final IOException e) {
            platformNames = new LinkedList<>();
        }
        final List<Platform> platforms = new LinkedList<>();
        for (final String name : platformNames)
            platforms.add(Platform.get(name));
        return platforms;
    }

    private static void save(final Iterable<Platform> platforms) {

        final Collection<String> platformNames = new LinkedList<>();
        for (final Platform p : platforms)
            platformNames.add(p.getString());
        new File(PLATFORMS_).getParentFile().mkdirs(); // Create parent directory
        try {
            Files.writeString(Paths.get(PLATFORMS_),
                    StringUtils.join(platformNames, _COMMA_));
        } catch (final IOException e) {
            throw new RuntimeException(b(c(UNABLE), TO, WRITE, INTO, PLATFORMS, FILE, q(PLATFORMS_)));
        }
    }

    @SuppressWarnings("ObjectAllocationInLoop")
    private static List<Platform> scan() {

        final List<Platform> platforms = new LinkedList<>();
        WebDriver driver;
        for (final Platform platform : Platform.values())
            if (Platform.ANDROID == platform) {
                try {
                    final AppiumDriverLocalService service = new AppiumServiceBuilder().withArgument(
                                    () -> __BASE_PATH, WD_HUB)
                            .withLogFile(new File(APPIUM_LOG_)).build();
                    service.start();
                    final File appDir = new File(new File(System.getProperty(USER_DIR)), s(_DOT_));
                    final DesiredCapabilities capabilities = getDesiredCapabilities(appDir);
                    driver = new AndroidDriver(service.getUrl(), capabilities);
                    platforms.add(Platform.ANDROID);
                    driver.close();
                    service.close();
                } catch (final Exception e) {
                    // d.close();
                }
            } else try {
                driver = WebDrivers.get(platform);
                platforms.add(platform);
                driver.close();
            } catch (final Exception e) {
                // d.close();
            }
        return platforms;
    }
}
