package examples.seven;

import li.chee.reactive.plumber.Plumbing;
import reactor.core.publisher.Flux;

public abstract class Seven extends Plumbing {

    static Flux<Integer> input = range(1, 300);
}
