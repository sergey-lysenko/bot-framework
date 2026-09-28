package works.lysenko.util.grid.record.gsrc;

import works.lysenko.util.apis.grid.t._Geometry;
import works.lysenko.util.apis.grid.t._Location;

/**
 * GridLocation represents the center point and radius of a grid.
 */
public record Geometry(_Location center, float radius, float ratio) implements _Geometry {

}
