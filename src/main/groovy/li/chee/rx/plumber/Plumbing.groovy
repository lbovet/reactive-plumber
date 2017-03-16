package li.chee.rx.plumber

import io.reactivex.Flowable
import io.reactivex.Single
import io.reactivex.flowables.GroupedFlowable
import io.reactivex.parallel.ParallelFlowable
import io.reactivex.schedulers.Schedulers

/**
 * Base piping tools.
 */
abstract class Plumbing extends Flowable {

    // Box utilities
    static wrap = Box.&wrap
    static unwrap = Box.&unwrap
    static attach = Box.&attach
    static mapper = Box.&mapper
    static bind = Box.&flatMap
    static show = { println it }

    static export(Object... objects) {
        return objects
    }

    /**
     * Resolves a source. It can be a Flowable or a closure returning a Flowable.
     * @param it the source or its generating function.
     * @return te Flowable
     */
    static Flowable from(it) {
        Closure.isAssignableFrom(it.getClass()) ? it() :it
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
    static Flowable pipe(Closure closure) {
        def result = closure()
        if (ParallelFlowable.isAssignableFrom(result.getClass())) {
            result = result.sequential()
        }
        result = result.subscribeOn Schedulers.computation()
        if (Single.isAssignableFrom(result.getClass())) {
            result = result.toFlowable()
        }
        result
    }

    /**
     * Parallelizes processing on the computation scheduler.
     * @param input a flowable to parallelize
     * @return the parallelized flowable
     */
    static ParallelFlowable parallel(input) {
        input.parallel().runOn(Schedulers.computation())
    }

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
                pipe { closure(f) }
            }
        }
    }

    /**
     * Marks pipes to be terminal, so that they get subscribed in a blocking way to avoid exiting before finishing work.
     * @param pipes the terminal pipes
     * @return nothing
     */
    static drain(Flowable... pipes) {
        def lasts = fromIterable(pipes.toList()) map { it.last('').toFlowable() }
        merge(lasts).blockingLast()
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
