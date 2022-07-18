package li.chee.reactive.plumber

import io.reactivex.Flowable
import io.reactivex.Single
import io.reactivex.flowables.GroupedFlowable
import io.reactivex.functions.Consumer
import io.reactivex.parallel.ParallelFlowable
import io.reactivex.schedulers.Schedulers
import org.reactivestreams.Publisher

import java.util.concurrent.CountDownLatch

/**
 * Base piping tools.
 */
abstract class Plumbing extends Flowable {

    static <T> Flowable<T> value(Flowable<T> f) {
        f.firstElement().cache().repeat()
    }

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

    /**
     * Resolves a source. It can be a Flowable or a closure returning a Flowable.
     * @param it the source or its generating function.
     * @return te Flowable
     */
    static <T> Flowable<T> from(Closure<Flowable<T>> closure) {
        closure()
    }

    static <T> Flowable<T> from(Flowable<T> f) {
        f
    }

    /**
     * Extract the GroupedFlowable key.
     * @param it
     * @return the key
     */
    static key(it) {
        it.getKey()
    }

    /**
     * Composes a Flowable from a closure that returns a Flowable or a Single.
     * It also sequentializes parallelized Flowables.
     * @param input a a closure
     * @param a closure
     * @return a Flowable
     */
    static <T> Flowable<T> pipe(Closure<? extends Object> closure) {
        def result = closure()
        result = normalize result
        publish(result)
    }

    private static <T> Flowable<T> normalize(f) {
        if (ParallelFlowable.isAssignableFrom(f.getClass())) {
            ((ParallelFlowable)f).sequential()
        } else if (Single.isAssignableFrom(f.getClass())) {
            ((Single) f).toFlowable()
        } else {
            ((Flowable<T>)f)
        }
    }

    static private <T> Flowable<T> publish(Flowable<T> f) {
        def result = f.publish()
        connectables.add result
        result
    }

    /**
     * Syntactic sugar when dealing with existing Flowables.
     * @param a Flowable
     * @return the same Flowable, untouched.
     */
    static <T> Flowable<T> pipe(Flowable<T> f) {
        f
    }

    /**
     * Makes a pipe cache already emitted event in order to replay them later, useful for {@code concat}.
     * Use it as a keyword just after {@code pipe}.
     *
     * @param a closure
     * @return a Flowable
     */
    static Flowable cache(Closure closure) {
        def result = closure().cache()
        result.subscribe()
        if(Single.isAssignableFrom(result.getClass())) {
            result = result.toFlowable()
        }
        result
    }

    /**
     * Parallelizes processing on the computation scheduler.
     * @param input a flowable to parallelize
     * @return the parallelized flowable
     */
    static parallel = { it.parallel().runOn(Schedulers.computation()) }

    static concurrent = { it.observeOn(Schedulers.computation()) }

    /**
     * Takes a flowable of flowables and apply a pipe to each one.
     *
     * @param streams
     * @param closure
     * @return
     */
    static each(Flowable streams, Closure closure ) {
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
    static drain(Flowable... pipes) {
        def latch = new CountDownLatch(pipes.size())
        def lasts = fromIterable(pipes.toList()) map { it.last('').toFlowable() }
        merge(lasts).subscribe((Consumer){ latch.countDown() })
        connectables.addAll pipes
        connectables.reverse().each { it.connect() }
        latch.await()
    }

    /**
     * Groups by a function returning a context and also add a mapper setting the context.
     * @param fn function returning a context
     * @return a grouped flowable
     */
    static groups(fn) {
        return { Flowable f -> f.map( { Box box -> box.with(fn(box)) }).groupBy(fn) }
    }

    /**
     * Split a flowable according to a list of predicates
     * @param predicates
     * @param f the flowable to split
     * @return
     */
    static split(predicates, f) {
        predicates.collect { predicate -> f.filter(predicate) }
    }

    /**
     * Function to set the context of a box to the key of a grouped flowable.
     * @param flowable
     * @return
     */
    static context(flowable) {
        return { Object it ->
            Box box
            if (it instanceof Box) {
                box = it
            } else {
                box = new Box(it)
            }
            return box.with(((GroupedFlowable)flowable).getKey())
        }
    }
}
