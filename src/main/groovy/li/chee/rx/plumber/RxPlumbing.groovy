package li.chee.rx.plumber

import reactor.core.publisher.*
import reactor.core.scheduler.Schedulers

import java.util.concurrent.CountDownLatch
import java.util.function.Consumer
import java.util.function.Function

/**
 * Base piping tools.
 */
abstract class Plumbing extends Flux {

    // Box utilities
    static wrap = Box.&wrap
    static unwrap = Box.&unwrap
    static attach = Box.&attachFlux
    static mapper = Box.&mapper
    static bind = Box.&flatMap
    static show() {
        show(null)
    }
    static show(x) {
        x = x == null ? "": " "+x+":"
        return { println "("+Thread.currentThread().getId()+")"+x+" "+it }
    }

    private static connectables = []

    static export(Object... objects) {
        return objects
    }

    private static Flux internalFrom(it) {
        it = Closure.isAssignableFrom(it.getClass()) ? it() :it
        if(!ParallelFlux.isAssignableFrom(it.getClass())) {
            it = ((Flux)it).publishOn(Schedulers.parallel())
        }
        it
    }

    /**
     * Resolves a source. It can be a Flux or a closure returning a Flux.
     * @param it the source or its generating function.
     * @return te Flux
     */
    static Flux from(it) {
          internalFrom it
    }

    static Flux from(Flux f) {
        internalFrom f
    }

    /**
     * Extract the GroupedFlux key.
     * @param it
     * @return the key
     */
    static key(it) {
        it.key()
    }

    /**
     * Composes a Flux from a closure that returns a Flux or a Single.
     * It also sequentializes parallelized Fluxs.
     * @param input a a closure
     * @param a closure
     * @return a Flux
     */
    static Flux pipe(Closure closure) {
        def result = closure()
        result = normalize result
        result = result.publish()
        connectables.add result
        result
    }

    /**
     * Syntactic sugar when dealing with existing Fluxs.
     * @param a Flux
     * @return the same Flux, untouched.
     */
    static Flux pipe(Flux f) {
        f
    }

    /**
     * Makes a pipe cache already emitted event in order to replay them later, useful for {@code concat}.
     * Use it as a keyword just after {@code pipe}.
     *
     * @param a closure
     * @return a Flux
     */
    static Flux cache(Closure closure) {
        def result = closure().cache()
        result.subscribe()
        if(Mono.isAssignableFrom(result.getClass())) {
            result = result.flux()
        }
        result
    }

    /**
     * Parallelizes processing on the computation scheduler.
     * @param input a flux to parallelize
     * @return the parallelized flux
     */
    static ParallelFlux parallel(input) {
        input.parallel().runOn(Schedulers.parallel())
    }

    /**
     * Takes a flux of fluxs and apply a pipe to each one.
     *
     * @param streams
     * @param closure
     * @return
     */
    static each(Flux streams, Closure closure ) {
        return {
            streams.map { f ->
                normalize closure(f)
            }
        }
    }

    /**
     * Marks pipes to be terminal, so that they get subscribed in a blocking way to avoid exiting before finishing work.
     * @param pipes the terminal pipes
     * @return nothing
     */
    static drain(Flux... pipes) {
        def latch = new CountDownLatch(pipes.size())
        def lasts = fromIterable(pipes.toList()) map { it.last('').flux() }
        merge(lasts).subscribe((Consumer){ latch.countDown() })
        pipes.findAll { it instanceof ConnectableFlux } forEach { connectables.add it }
        connectables.reverse().each { ((ConnectableFlux)it).connect() }
        connectables.clear()
        latch.await()
    }

    /**
     * Groups by a function returning a context and also add a mapper setting the context.
     * @param fn function returning a context
     * @return a grouped flux
     */
    static Function groups(fn) {
        return { Flux f -> f.map( { Box box -> box.with(fn(box)) }).groupBy((Function)fn) }
    }

    /**
     * Split a flux according to a list of predicates
     * @param predicates
     * @param f the flux to split
     * @return
     */
    static split(predicates, f) {
        predicates.collect { predicate -> f.filter(predicate) }
    }

    /**
     * Function to set the context of a box to the key of a grouped flux.
     * @param flux
     * @return
     */
    static context(flux) {
        return { Object it ->
            Box box
            if (it instanceof Box) {
                box = it
            } else {
                box = new Box(it)
            }
            return box.with(((GroupedFlux)flux).key())
        }
    }

    private static Flux normalize(f) {
        if (ParallelFlux.isAssignableFrom(f.getClass())) {
            f.sequential()
        } else if (Mono.isAssignableFrom(f.getClass())) {
            f.flux()
        } else {
            f
        }
    }
}
