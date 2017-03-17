package examples.two

import static examples.two.Tools.*

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

def odd = pipe cache {
    from marked \
    filter context(ODD) \
    count() \
    map with(ODD)
}

def result = pipe {
    from concat(even, odd) doOnNext show("total")
}

drain result


