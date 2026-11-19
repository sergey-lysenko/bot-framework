package works.lysenko.util.apis.execution;

import works.lysenko.util.data.enums.Platform;

import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;

/**
 * SPI for creating {@link _Driver} instances and scanning locally available execution {@link Platform}s.
 */
public interface _DriverProvider {

    /**
     * Returns the first registered {@link _DriverProvider} on the classpath, if {@code framework-driver} is present.
     *
     * @return optional driver provider
     */
    static Optional<_DriverProvider> find() {
        return Holder.PROVIDER;
    }

    /**
     * Instantiates a new uninitialised {@link _Driver} using the first registered provider on the classpath.
     *
     * @return a new {@link _Driver} instance, or {@code null} if no provider is registered
     */
    static _Driver createDriver() {
        return Holder.PROVIDER
                .map(_DriverProvider::create)
                .orElse(null);
    }

    /**
     * Scans locally installed browser/mobile drivers using the first registered provider on the classpath.
     *
     * @return ordered list of available platforms, or an empty list if no provider is registered
     */
    static List<Platform> scanPlatforms() {
        return Holder.PROVIDER
                .map(_DriverProvider::scanAvailablePlatforms)
                .orElse(List.of());
    }

    /**
     * Creates a new {@link _Driver} instance.
     *
     * @return new driver wrapper
     */
    _Driver create();

    /**
     * Detects locally available browser and mobile platforms.
     *
     * @return ordered list of available platforms
     */
    List<Platform> scanAvailablePlatforms();

    /**
     * Lazy holder for classpath-discovered driver provider.
     */
    final class Holder {
        static final Optional<_DriverProvider> PROVIDER =
                ServiceLoader.load(_DriverProvider.class).findFirst();

        private Holder() {
        }
    }
}
