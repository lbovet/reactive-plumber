package li.chee.rx.plumber

import io.reactivex.Flowable
import io.reactivex.Single
import io.reactivex.parallel.ParallelFlowable
import io.reactivex.schedulers.Schedulers

import java.util.concurrent.CountDownLatch

/**
 * Base piping tools.
 */
class Plumbing {

    // The plumbing to remember
    private static pipes = []
    private static sinks = []

    /**
     * Resolves a source. It can be a Flowable or a function returning a Flowable.
     * @param it
     * @return a Flowable
     */
    static Flowable from(it) {
        if (Flowable.isAssignableFrom(it.getClass())) {
            it
        } else {
            it()
        }
    }

    /**
     * Builds a ConnectableFlowable from a closure result. Tranforms Flowables and Singles.
     * Teminates parallelized Flowables.
     * This also registers the ConnectableFlowable for being connected on done().
     * @param a closure
     * @return a ConnectableFlowable
     */
    static Flowable pipe(it) {
        def result = it()
        if (ParallelFlowable.isAssignableFrom(result.getClass())) {
            result = result.sequential()
        }
        //result = result.observeOn Schedulers.newThread()
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

    static ParallelFlowable parallel(Flowable it) {
        it.parallel() runOn Schedulers.computation()
    }

    /**
     * A terminal pipe. It will be registered so that the main thread will wait on it to be terminated.
     * @param a closure
     * @return nothing
     */
    static sink(it) {
        sinks.add pipe(it)
    }

    /**
     * Connects the registered pipes in reverse order of declaration.
     * Also observe the sinks to wait blockingly on their last item so that the process does not terminate too early.
     * @return nothing
     */
    static done() {
        CountDownLatch latch = new CountDownLatch(sinks.size())
        def lasts = Flowable.fromIterable((Iterable<Flowable>) sinks) map { it.last('-').toFlowable() }
        Flowable.merge(lasts).subscribe { latch.countDown() }
        pipes.reverse().each { it.connect() }
        latch.await()
    }
}
