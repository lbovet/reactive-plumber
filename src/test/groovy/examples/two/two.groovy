package examples.two

import static examples.two.Tools.*
import static io.reactivex.Flowable.merge
import static li.chee.rx.plumber.Plumbing.*

def marked = pipe {
    from input \
    map parity
}

def even = pipe {
    from marked \
    filter context(EVEN) \
    count() \
    map with(EVEN)
}

def odd = pipe {
    from marked \
    filter context(ODD) \
    count() \
    map with(ODD)
}

def result = pipe {
    from merge(even, odd) doOnNext show("total")
}

drain result


