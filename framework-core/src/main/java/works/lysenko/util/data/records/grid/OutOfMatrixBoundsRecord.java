package works.lysenko.util.data.records.grid;

import works.lysenko.util.data.records.Location;

/**
 * Record representing a point outside matrix bounds along with its integer grid indices.
 *
 * @param location the floating-point location
 * @param iX       the integer X index
 * @param iY       the integer Y index
 */
record OutOfMatrixBoundsRecord(Location location, int iX, int iY) {}
