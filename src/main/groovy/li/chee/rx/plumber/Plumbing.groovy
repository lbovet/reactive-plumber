package li.chee.rx.plumber

import io.reactivex.Flowable
import io.reactivex.Single
import io.reactivex.parallel.ParallelFlowable
import io.reactivex.schedulers.Schedulers

/**
 * Pipes and tools.
 */
class Plumbing {

    private static pipes = []
    private static sinks = []

    static wrap = Box.&wrap
    static context = Box.&context
    static Flowable from(it) { it }

    static done() {
        pipes.reverse().each { it.connect() }
        def i = Flowable.fromArray((Flowable[])sinks) map { it.last('').toFlowable() }
        Flowable.merge(i).blockingLast();
    }

    static Flowable pipe(it) {
        def result = it()
        if (ParallelFlowable.isAssignableFrom(result.getClass())) {
            result = result.sequential()
        }
        result = result.observeOn Schedulers.newThread()
        if (Flowable.isAssignableFrom(result.getClass())) {
            result = result.publish()
        } else if (Single.isAssignableFrom(result.getClass())) {
            result = result.toFlowable().publish()
        } else {
            return null
        }
        pipes.add result
        result
    }

    static sink(it) {
        sinks.add pipe(it)
    }

    static ParallelFlowable parallel(Flowable it) {
        it.parallel() runOn Schedulers.computation()
    }

}
