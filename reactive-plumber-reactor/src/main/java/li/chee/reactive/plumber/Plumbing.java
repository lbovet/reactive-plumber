package li.chee.reactive.plumber;

import groovy.lang.Closure;
import org.reactivestreams.Publisher;
import reactor.core.publisher.*;
import reactor.core.scheduler.Schedulers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

public abstract class Plumbing extends Flux {

    private static List<ConnectableFlux<?>> connectables = new ArrayList<>();

    public static <T> Flux<T> value(Flux<T> f) {
        Mono<T> cached = f.next().cache();
        cached.subscribe();
        return cached.repeat();
    }

    public static <V> Consumer<V> show() {
        return show(null);
    }

    public static <V> Consumer<V> show(String prefix) {
        final String actualPrefix = prefix == null ? "": " "+prefix+":";
        return (V obj) -> System.out.println("("+Thread.currentThread().getId()+")"+actualPrefix+" "+obj.toString());
    }

    public static <T, U extends Flux<T>> Flux<T> from(U f) {
        return f;
    }

    public static <T> Flux<T> from(Object f, Class<T> c) {
        return ((Flux<?>)f).cast(c);
    }

    public static Flux<?> from(Object f) {
        return (Flux<?>)f;
    }

    public static <T> Flux<T> pipe(Closure<? extends Publisher<T>> closure) {
        ConnectableFlux<T> result = normalize(closure.call()).publish();
        connectables.add(result);
        return result;
    }

    public static <T> Flux<T> tube(Closure<? extends Publisher<T>> closure) {
        return normalize(closure.call());
    }

    public static <T, U extends Flux<T>> U pipe(U f) {
        return f;
    }

    public static <T> Flux<T> cache(int size, Closure<? extends Publisher<T>> closure) {
        Publisher<T> f = closure.call();
        if (f instanceof Mono) {
            Mono<T> m = ((Mono<T>) f).cache();
            m.subscribe();
            return m.flux();
        } else {
            ConnectableFlux<T> n = normalize(closure.call()).replay(size);
            connectables.add(n);
            return n;
        }
    }

    public static <T, U extends Flux<T>> ParallelFlux<T> parallel(U f) {
        return f.parallel().runOn(Schedulers.parallel());
    }

    public static <T> Function<Flux<T>, Flux<T>> fork() {
        return (f) -> f.publishOn(Schedulers.elastic());
    }

    public static <T> Flux<T> concurrent(Flux<T> f) {
        return f.publishOn(Schedulers.parallel());
    }

    public static <K,T> Flux<Flux<T>> each(Flux<? extends GroupedFlux<K,T>> streams, Closure<? extends Publisher<T>> closure) {
        return streams.map((f) -> normalize(closure.call(f)));
    }

    @SuppressWarnings("unchecked")
    public static <K, V> K key(Object f) {
        return ((GroupedFlux<K, V>)f).key();
    }

    public static <T> Iterable<Flux<T>> split(Iterable<Predicate<T>> predicates, Flux<T> f) {
        return fromIterable(predicates).map(f::filter).toIterable();
    }

    public static <T> Junction<T> join(Flux<T> flux) {
        return new Junction<>(flux);
    }

    static class Junction<T> {
        private Flux<T> t;
        Junction(Flux<T> t) {
            this.t = t;
        }
        public void to(List<Flux<T>> fluxes) {
            fluxes.add(t);
        }
    }

    public static void drain(Flux<?>... pipes) {
        CountDownLatch latch = new CountDownLatch(1);
        fromArray(pipes)
                .flatMap(Function.identity())
                .doOnComplete(latch::countDown)
                .subscribe();
        Collections.reverse(connectables);
        connectables.forEach(ConnectableFlux::connect);
        connectables.clear();
        try {
            latch.await();
        } catch (InterruptedException e) {
            // ignore
        }
    }

    private static <T> Flux<T> normalize(Publisher<T> f) {
        if (f instanceof ParallelFlux) {
            return ((ParallelFlux<T>)f).sequential();
        } else if (f instanceof Mono) {
            return ((Mono<T>)f).flux();
        } else {
            return Flux.from(f);
        }
    }
}
