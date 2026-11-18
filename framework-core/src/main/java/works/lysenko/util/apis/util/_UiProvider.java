package works.lysenko.util.apis.util;

import works.lysenko.base.Parameters;
import works.lysenko.util.apis.parameters._GUI;

import java.util.Optional;
import java.util.ServiceLoader;

/**
 * Service-provider bridge decoupling {@code framework-core} from optional Swing {@code framework-ui} components.
 */
public interface _UiProvider {

    /**
     * Returns the interactive Swing dashboard instance.
     *
     * @return dashboard instance
     */
    _Dashboard createDashboard();

    /**
     * Creates the interactive run-setup GUI bound to test parameters.
     *
     * @param parameters test parameters model
     * @return GUI controller
     */
    _GUI createSetupGui(Parameters parameters);

    /**
     * Returns the first registered {@link _UiProvider} on the classpath, if {@code framework-ui} is present.
     *
     * @return optional UI provider
     */
    static Optional<_UiProvider> find() {
        return Holder.PROVIDER;
    }

    /**
     * Lazy holder for classpath-discovered UI provider.
     */
    final class Holder {
        static final Optional<_UiProvider> PROVIDER =
                ServiceLoader.load(_UiProvider.class).findFirst();

        private Holder() {
        }
    }
}
