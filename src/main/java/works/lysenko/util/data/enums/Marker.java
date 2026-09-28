package works.lysenko.util.data.enums;

import works.lysenko.util.apis.data._Marker;

import static works.lysenko.util.chrs.__.TO;
import static works.lysenko.util.chrs.____.FROM;

@SuppressWarnings({"MissingJavadoc", "Singleton"})
public enum Marker implements _Marker {

    FROM_TO(FROM, TO);

    private final String first;
    private final String second;

    Marker(final String first, final String second) {

        this.first = first;
        this.second = second;
    }

    public String getFirst() {

        return first;
    }

    public String getSecond() {

        return second;
    }
}
