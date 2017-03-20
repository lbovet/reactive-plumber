package examples.two

import static Tools.*

def marked = pipe {
    from Tools.input \
    map Tools.parity
}

def even = pipe {
    from marked \
    filter context(Tools.EVEN) \
    count() \
    map with(Tools.EVEN)
}

def odd = pipe cache {
    from marked \
    filter context(Tools.ODD) \
    count() \
    map with(Tools.ODD)
}

def result = pipe {
    from concat(even, odd) doOnNext show("total")
}

drain result


