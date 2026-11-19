package works.lysenko.base.exec;

import io.appium.java_client.service.local.AppiumDriverLocalService;
import io.appium.java_client.service.local.AppiumServiceBuilder;
import io.github.bonigarcia.wdm.WebDriverManager;
import works.lysenko.util.apis.execution._Driver;
import works.lysenko.util.apis.execution._DriverProvider;
import works.lysenko.util.data.enums.Platform;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import static works.lysenko.base.util.Platforms.__BASE_PATH;
import static works.lysenko.util.lang.W.WD_HUB;
import static works.lysenko.util.spec.Layout.Files.APPIUM_LOG_;

/**
 * Default {@link _DriverProvider} implementation backed by Selenium WebDriver, WebDriverManager, and Appium.
 */
public final class DefaultDriverProvider implements _DriverProvider {

    @Override
    public _Driver create() {
        return new Driver();
    }

    @Override
    @SuppressWarnings("ObjectAllocationInLoop")
    public List<Platform> scanAvailablePlatforms() {
        final List<Platform> platforms = Collections.synchronizedList(new LinkedList<>());
        Arrays.stream(Platform.values()).parallel().forEach(platform -> {
            if (Platform.ANDROID == platform) {
                try {
                    final AppiumDriverLocalService service = new AppiumServiceBuilder()
                            .withArgument(() -> __BASE_PATH, WD_HUB)
                            .usingAnyFreePort()
                            .withLogFile(new File(APPIUM_LOG_))
                            .build();
                    if (null != service) {
                        platforms.add(Platform.ANDROID);
                    }
                } catch (final Exception ignored) {
                    // Appium executable is not installed or available
                }
            } else {
                try {
                    if (isDriverAvailable(platform)) {
                        platforms.add(platform);
                    }
                } catch (final Exception ignored) {
                    // Driver not available or failed to initialize
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

    private static boolean isDriverAvailable(final Platform platform) {
        switch (platform) {
            case CHROME -> {
                final WebDriverManager wdm = WebDriverManager.chromedriver();
                wdm.setup();
                return null != wdm.getDownloadedDriverPath();
            }
            case FIREFOX -> {
                final WebDriverManager wdm = WebDriverManager.firefoxdriver();
                wdm.setup();
                return null != wdm.getDownloadedDriverPath();
            }
            case EDGE -> {
                final WebDriverManager wdm = WebDriverManager.edgedriver();
                wdm.setup();
                return null != wdm.getDownloadedDriverPath();
            }
            case SAFARI -> {
                final WebDriverManager wdm = WebDriverManager.safaridriver();
                wdm.setup();
                return null != wdm.getDownloadedDriverPath();
            }
            default -> {
                return false;
            }
        }
    }
}
