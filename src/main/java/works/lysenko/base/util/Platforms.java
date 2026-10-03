package works.lysenko.base.util;

import io.appium.java_client.service.local.AppiumDriverLocalService;
import io.appium.java_client.service.local.AppiumServiceBuilder;
import org.apache.commons.lang3.StringUtils;
import org.openqa.selenium.WebDriver;
import works.lysenko.util.data.enums.Platform;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import static works.lysenko.util.chrs.__.TO;
import static works.lysenko.util.chrs.____.*;
import static works.lysenko.util.data.strs.Bind.b;
import static works.lysenko.util.data.strs.Bind.d;
import static works.lysenko.util.data.strs.Case.c;
import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.data.strs.Wrap.q;
import static works.lysenko.util.lang.W.WD_HUB;
import static works.lysenko.util.lang.word.P.PLATFORMS;
import static works.lysenko.util.lang.word.U.UNABLE;
import static works.lysenko.util.lang.word.W.WRITE;
import static works.lysenko.util.prop.core.Start.forcedPlatform;
import static works.lysenko.util.prop.data.Delimeters.L1;
import static works.lysenko.util.spec.Layout.Files.APPIUM_LOG_;
import static works.lysenko.util.spec.Layout.Files.PLATFORMS_;
import static works.lysenko.util.spec.Symbols._COMMA_;
import static works.lysenko.util.spec.Symbols._DASH_;


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

        final List<Platform> platforms = Collections.synchronizedList(new LinkedList<>());
        Arrays.stream(Platform.values()).parallel().forEach(platform -> {
            if (Platform.ANDROID == platform) {
                AppiumDriverLocalService service = null;
                try {
                    service = new AppiumServiceBuilder()
                            .withArgument(() -> __BASE_PATH, WD_HUB)
                            .usingAnyFreePort()
                            .withLogFile(new File(APPIUM_LOG_))
                            .build();
                    service.start();
                    if (service.isRunning()) {
                        platforms.add(Platform.ANDROID);
                    }
                } catch (final Exception ignored) {
                    // Appium service failed to start or is not available
                } finally {
                    if (null != service && service.isRunning()) {
                        service.stop();
                    }
                }
            } else {
                WebDriver driver = null;
                try {
                    driver = WebDrivers.get(platform, false, true);
                    if (null != driver) {
                        platforms.add(platform);
                    }
                } catch (final Exception ignored) {
                    // Driver not available or failed to initialize
                } finally {
                    if (null != driver) {
                        try {
                            driver.quit();
                        } catch (final Exception ignored) {
                            // Driver already terminated
                        }
                    }
                }
            }
        });
        final List<Platform> sorted = new LinkedList<>();
        for (final Platform p : Platform.values()) {
            if (platforms.contains(p)) {
                sorted.add(p);
            }
        }
        return sorted;
    }
}
