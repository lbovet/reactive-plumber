package examples.three

import static Three.*

def numbers = pipe {
    from input \
    map fizzbuzz
}

def groups = pipe {
    from numbers \
    groupBy context
}

def counts = pipe each(groups) {
    from it \
    count() \
    map with(key(it))
}

def statistics = pipe {
    from concat(counts) \
    doOnNext stats
}

def outputd = pipe {
    from numbers \
    doOnNext display
}

drain outputd, statistics
