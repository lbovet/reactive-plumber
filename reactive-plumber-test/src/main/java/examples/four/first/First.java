package examples.four.first;

import li.chee.reactive.plumber.Plumbing;
import reactor.core.publisher.Flux;

import java.util.function.Predicate;

public abstract class First extends Plumbing {

    public static class Exports {
        public Flux<Integer> even = null;
        public Flux<Integer> odd = null;
    }

    public static Exports exports = new Exports();

    static Flux<Integer> input = just(1, 2, 3, 4);

    static Predicate<Integer> even = x -> x % 2 == 0;
    static Predicate<Integer> odd = x -> x % 2 != 0;
}
