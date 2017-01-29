package li.chee.rx.plumber

import io.reactivex.Flowable
import io.reactivex.Single
import io.reactivex.parallel.ParallelFlowable
import io.reactivex.schedulers.Schedulers

input = Flowable.range(1, 4)
def wrap = Box.&wrap
def context = Box.&context
def print = { System.out.println it }
def render = { it.getContext(String.class) + " " + Thread.currentThread().getName() + " " + it.getValue() }

def Flowable from(it) { it }

pipes = []
sinks = []

def done() {
    pipes.reverse().each { it.connect() }
    i = Flowable.fromArray((Flowable[])sinks) map { it.last('').toFlowable() }
    Flowable.merge(i).blockingLast();
}

def Flowable pipe(it) {
    result = it()
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

def sink(it) {
    sinks.add pipe(it)
}

def ParallelFlowable parallel(Flowable it) {
    it.parallel() runOn Schedulers.computation()
}

// ---------------------

def data = pipe {
    from input  \
    map wrap  \
    map context("hello")
}

def renderer = pipe {
    parallel from(data) \
    map render \
}

def count = pipe {
    from renderer \
    count()
}

sink {
    from renderer \
    concatWith count  \
    doOnNext print
}

sink {
    from renderer \
    doOnNext { System.out.println "hello" }
}

done()
