package examples.three;

import li.chee.reactive.plumber.Box;
import li.chee.reactive.plumber.Plumbing;
import reactor.core.publisher.Flux;

import java.util.function.Consumer;
import java.util.function.Function;

public abstract class Three extends Plumbing {

    protected static Flux<Box<Integer>> input = range(1, 15).map(Box::wrap);

    protected static Consumer<Box<Integer>> display =  box -> {
        FizzBuzz context = box.getContext(FizzBuzz.class);
        System.out.println(context == FizzBuzz.NONE ? box.getValue() : context);
    };

    protected static Function<Long,Box<Long>> with(Object context) {
        return value -> Box.wrap(value).with(context);
    }

    @SuppressWarnings("unchecked")
    protected static Flux<Box<Integer>> integers(Object f) {
        return (Flux<Box<Integer>>)f;
    }

    protected static Function<Box<Integer>,Box<Integer>> fizzbuzz = box -> {
        boolean fizz = box.getValue() % 3 == 0;
        boolean buzz = box.getValue() % 5 == 0;
        boolean both = fizz && buzz;
        return box.with( both ? FizzBuzz.FIZZBUZZ: fizz ? FizzBuzz.FIZZ : buzz ? FizzBuzz.BUZZ : FizzBuzz.NONE );
    };

    protected static Function<Box<Integer>,FizzBuzz> context =
            box -> box.getContext(FizzBuzz.class);

    protected static Consumer<Box<Long>> stats = box -> {
        if(box.getContext(FizzBuzz.class) != FizzBuzz.NONE) {
            System.out.println(" => " +box.getContext(FizzBuzz.class).toString() + ": " + box.getValue());
        }
    };

    protected enum FizzBuzz { NONE, FIZZ, BUZZ, FIZZBUZZ }
}
