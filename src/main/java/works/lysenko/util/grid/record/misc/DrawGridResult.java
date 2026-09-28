package works.lysenko.util.grid.record.misc;

import java.awt.image.BufferedImage;

/**
 * DrawGridResult is a record representing the result of drawing a grid on an image.
 */
public record DrawGridResult(BufferedImage image, int samples, int area) {}
