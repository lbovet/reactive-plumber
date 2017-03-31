package examples.eight

import static li.chee.reactive.plumber.Plumbing.*
import static reactor.core.publisher.Flux.*

def tub = tube {
    fromIterable([ range(-400,400), range(0,300)]) \
    flatMap({ it }, 1, 1)
}

def one = tube {
    from tub \
    count() \
    doOnNext show("one")
}

def two = tube {
    from tub \
    doOnNext show("two")
}

def end = pipe {
    from merge(one,two) \
    doOnNext show("merged")
}

drain end
