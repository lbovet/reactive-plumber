package examples.five;

import li.chee.reactive.plumber.Plumbing;
import reactor.core.publisher.Flux;

import java.util.Arrays;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Predicate;

public abstract class Five extends Plumbing {
    protected static Flux<Object> input = just("hello", 5, 3, "world");

    protected static List<Predicate> types = Arrays.asList(
            x -> x instanceof String,
            x -> x instanceof Integer
    );

    protected static BiFunction<String, String, String> line = (x, y) -> x + " " + y;
}
