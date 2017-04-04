package examples.four.third;

import li.chee.reactive.plumber.Plumbing;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;

public abstract class Third extends Plumbing {
    public static class Exports {
        public List<Flux<String>> strings = new ArrayList<>();
    }

    public static Exports exports = new Exports();
}
