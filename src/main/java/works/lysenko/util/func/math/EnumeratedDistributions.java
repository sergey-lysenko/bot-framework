package works.lysenko.util.func.math;

import org.apache.commons.math3.distribution.EnumeratedDistribution;
import org.apache.commons.math3.util.Pair;
import works.lysenko.util.data.records.KeyValue;

import java.util.Collection;
import java.util.stream.Collectors;

public record EnumeratedDistributions<T>() {

    /**
     * Creates an EnumeratedDistribution based on the input list of KeyValue pairs.
     *
     * @param users The list of KeyValue pairs representing the distribution.
     * @param <T>   The type of the elements in the EnumeratedDistribution.
     * @return An EnumeratedDistribution instance based on the input distribution.
     */
    public static <T> EnumeratedDistribution<T> enumDis(final Collection<KeyValue<T, Double>> users) {

        return new EnumeratedDistribution<>(users.stream()
                .map(kv -> new Pair<>(kv.k(), kv.v()))
                .collect(Collectors.toList()));
    }
}
