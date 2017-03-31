package examples.seven

import static examples.seven.Seven.input
import static li.chee.reactive.plumber.Plumbing.*
import static reactor.core.publisher.Flux.concat


def source = tube {
    from input \
    doOnNext show("input") \
}

def data = pipe {
    from source
}

def multiple = tube {
    from data \
    doOnNext show("multiple")
}

def left = pipe {
    from multiple \
    doOnNext show("left")
}

def right = pipe cache(1000) {
    from multiple \
    doOnNext show("right")
}

def all = pipe {
    from concat(left, right) \
    doOnNext show("merged")
}

drain all
