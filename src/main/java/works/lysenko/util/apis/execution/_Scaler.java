package works.lysenko.util.apis.execution;

import org.apache.commons.math3.fraction.Fraction;

import java.awt.image.BufferedImage;

/**
 * Represents a utility for scaling images based on the current screen resolution.
 */
@SuppressWarnings("InterfaceWithOnlyOneDirectInheritor")
public interface _Scaler {

    /**
     * Retrieves the scale compensation.
     *
     * @return The Fraction representing the scale compensation.
     */
    Fraction getCompensation();

    /**
     * Retrieves the current scale.
     *
     * @return The Fraction representing the current scale.
     */
    Fraction getCurrent();

    /**
     * Retrieves an image.
     *
     * @return The BufferedImage retrieved.
     */
    BufferedImage getImage();

    /**
     * Retrieves the native scale.
     *
     * @return The Fraction representing the native scale.
     */
    @SuppressWarnings("unused")
    Fraction getNative();

    /**
     * Checks if the current state of the scaler is valid and operational.
     *
     * @return {@code true} if the state is valid and operational, otherwise {@code false}.
     */
    boolean isOk();
}
