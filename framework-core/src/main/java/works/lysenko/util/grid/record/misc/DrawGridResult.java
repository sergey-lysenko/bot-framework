package works.lysenko.util.grid.record.misc;

import java.awt.image.BufferedImage;

/**
 * DrawGridResult is a record representing the result of drawing a grid on an image.
 *
 * @param image   the resulting buffered image with grid drawn
 * @param samples the number of samples processed
 * @param area    the area coverage
 */
public record DrawGridResult(BufferedImage image, int samples, int area) {}
