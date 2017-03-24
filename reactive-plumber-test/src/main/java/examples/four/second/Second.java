package examples.four.second;

import examples.four.first.First;
import li.chee.reactive.plumber.Plumbing;

import java.util.function.BiFunction;
import java.util.function.Function;

public abstract class Second extends Plumbing {
    static First.Exports first = First.exports;

    static Function<Integer,String> toString = x -> x.toString();

    static BiFunction<String,String,String> line = (a, b) -> a + " " + b;

}
