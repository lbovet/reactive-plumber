package examples.four.second;

import examples.four.first.First;
import examples.four.third.Third;
import li.chee.reactive.plumber.Plumbing;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

public abstract class Second extends Plumbing {
    static First.Exports first = First.exports;
    static Third.Exports third = Third.exports;

    static Function<Integer,String> toString = x -> x.toString();

    static BiFunction<String,String,String> line = (a, b) -> a + " " + b;

}
