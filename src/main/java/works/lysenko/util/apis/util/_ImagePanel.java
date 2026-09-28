package works.lysenko.util.apis.util;

import java.awt.Graphics;
import java.awt.Image;

/**
 * The _ImagePanel interface represents a image panel that can be painted on the screen and
 * display an image. It provides methods for setting the image to be displayed and adjusting
 * the scale of the image.
 */
@SuppressWarnings({"WeakerAccess", "InterfaceWithOnlyOneDirectInheritor"})
public interface _ImagePanel {

    /**
     * This method is responsible for painting the component on the screen.
     *
     * @param g the Graphics object used for painting
     */
    @SuppressWarnings("unused")
    void paintComponent(Graphics g);

    /**
     * Sets the image to be displayed on the imagePanel component. The image is assigned to the 'image' variable of the
     * imagePanel.
     * It also updates the preferred size of the imagePanel to accommodate the new image dimensions.
     *
     * @param img the image to be set on the imagePanel
     */
    void setImage(final Image img);

    /**
     * Sets the scale of the ImagePanel.
     *
     * @param scale the scale factor to be applied to the image
     */
    void setScale(final float scale);

}
