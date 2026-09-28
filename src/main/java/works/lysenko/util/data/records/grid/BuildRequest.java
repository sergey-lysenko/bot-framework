package works.lysenko.util.data.records.grid;

import works.lysenko.util.apis.grid.d._Resolution;
import works.lysenko.util.apis.grid.t._Geometry;

import java.awt.image.BufferedImage;

/**
 * Represents the request parameters needed to construct a grid.
 * This immutable data record encapsulates the essential components
 * for grid construction, including the source image, geometric configuration,
 * resolution, and handling options for out-of-bounds regions.
 * <p>
 * Fields:
 * - image: The BufferedImage instance that serves as the base for grid construction.
 * - geometry: An implementation of the _Geometry interface, defining the geometric
 * parameters such as center, radius, and ratio for the grid.
 * - resolution: The resolution specification for the grid.
 * - ignoreOOB: A flag indicating whether out-of-bounds regions should be ignored
 * during the grid construction process.
 */
public record BuildRequest(BufferedImage image, _Geometry geometry, _Resolution resolution, boolean ignoreOOB) {}
