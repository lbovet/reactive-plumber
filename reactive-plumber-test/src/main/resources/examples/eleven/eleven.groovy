package examples.eleven

import reactor.core.scheduler.Schedulers

import static li.chee.reactive.plumber.Plumbing.*

def input = pipe {
    from range(0, 350) \
    doOnNext show("input")
}

def one = tube {
    from input \
    doOnNext show("one") \
    transform { just(it.toIterable()) }
}

def oneItem = tube {
    from one
}

oneItem.subscribeOn(Schedulers.elastic()).subscribe { def i=it.iterator(); while(i.hasNext()) { show("one-item").accept(i.next())}}

def two = tube {
    from input \
    doOnNext show("two") \
    transform { just(it.toIterable()) } }

def twoItem = tube {
    from two
}

twoItem.subscribeOn(Schedulers.elastic()).subscribe { def i=it.iterator(); while(i.hasNext()) { show("two-item").accept(i.next())}}

drain oneItem, twoItem
