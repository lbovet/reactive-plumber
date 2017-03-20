package examples.three

import static Tools.*

def numbers = pipe {
    from Tools.input \
    map Tools.fizzbuzz
}

def groups = pipe {
    from numbers \
    groupBy Tools.context
}

def counts = pipe each(groups) {
    from it \
    count() \
    map with(key(it))
}

def statistics = pipe {
    from concat(counts) \
    doOnNext Tools.stats
}

def output = pipe {
    from numbers \
    doOnNext Tools.display
}

drain output, statistics
