package examples.two;

import li.chee.reactive.plumber.Box;
import li.chee.reactive.plumber.Plumbing;
import reactor.core.publisher.Flux;

import java.util.function.Function;
import java.util.function.Predicate;

public abstract class Two extends Plumbing {
    protected static Flux<Box<Integer>> input = range(1, 5).map(Box::wrap);
    protected static Function<Box<Integer>, Box<Integer>> parity =
            box -> box.with(box.getValue() % 2 == 0 ? Parity.ODD : Parity.EVEN);
    protected static Predicate<Box<Integer>> only(Parity value) {
        return box -> box.getContext(value.getClass()) == value;
    }
    protected static Function<Long, Box<Long>> with(Parity parity) {
        return val -> Box.wrap(val).with(parity);
    }

    protected enum Parity {EVEN, ODD}
    protected static final Parity EVEN = Parity.EVEN;
    protected static final Parity ODD = Parity.ODD;
}
