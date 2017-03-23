package examples.one;

import li.chee.reactive.plumber.Box;
import li.chee.reactive.plumber.Plumbing;
import reactor.core.publisher.Flux;

import java.util.function.BiFunction;
import java.util.function.Function;

public abstract class One extends Plumbing {

    protected static Flux<Integer> input = Flux.just(1, 2, 3, 4);
    protected static Function<Integer, Box<Integer>> wrap = Box::wrap;
    protected static BiFunction<Box<Integer>, Long, Box<Integer>> attach = Box::attach;

    protected static Function<Box<Integer>,String> renderThread =
            s ->  s.getValue() + " ["+Thread.currentThread().getName() +"]";

    protected static Function<Box<Integer>,Box<String>> renderSize =
        box -> Box.wrap(box.getValue() + " / " + box.getContext(Long.class));
}
