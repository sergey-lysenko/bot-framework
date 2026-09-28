package works.lysenko.util.grid.record.rgbc;

import static java.util.Objects.isNull;

record Similar(int c1, int c2, double distance) {

    @SuppressWarnings("MethodWithMultipleReturnPoints")
    @Override
    public boolean equals(final Object obj) {

        if (this == obj) return true;
        if (isNull(obj) || getClass() != obj.getClass()) return false;

        final Similar similar = (Similar) obj;
        return c1 == similar.c1() && c2 == similar.c2() && 0 == Double.compare(distance, similar.distance());
    }

    @Override
    public int hashCode() {

        int result = c1;
        result = 31 * result + c2;
        result = 31 * result + Double.hashCode(distance);
        return result;
    }
}
