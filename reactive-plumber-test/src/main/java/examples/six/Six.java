package examples.six;

import javaslang.Tuple2;
import li.chee.reactive.plumber.Plumbing;
import reactor.core.publisher.Flux;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

public abstract class Six extends Plumbing {
    protected static Flux<String> input = just("foo", "world");

    protected static Function<Tuple2<List<String>,Integer>,List<String>> messages = Tuple2::_1;
    protected static Function<Tuple2<String,Integer>,Integer> length = Tuple2::_2;

    static Function<String,Tuple2<List<String>,Integer>> process =
        s -> new Tuple2<>(Arrays.asList("hello "+s, "how are you "+s+"?"), s.length());
}
