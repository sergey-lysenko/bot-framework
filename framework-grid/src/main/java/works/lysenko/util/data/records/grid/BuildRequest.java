package works.lysenko.util.data.records.grid;

import works.lysenko.util.apis.grid.d._Resolution;
import works.lysenko.util.apis.grid.t._Geometry;

import java.awt.image.BufferedImage;

/**
 * Represents the request parameters needed to construct a grid.
 * This immutable data record encapsulates the essential components
 * for grid construction, including the source image, geometric configuration,
 * resolution, and handling options for out-of-bounds regions.
 *
 * @param image      The BufferedImage instance that serves as the base for grid construction.
 * @param geometry   An implementation of the _Geometry interface, defining the geometric parameters for the grid.
 * @param resolution The resolution specification for the grid.
 * @param ignoreOOB  A flag indicating whether out-of-bounds regions should be ignored during grid construction.
 */
public record BuildRequest(BufferedImage image, _Geometry geometry, _Resolution resolution, boolean ignoreOOB) {}
