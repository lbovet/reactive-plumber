package examples.four

import static Tools.*

def categories = pipe {
    from input \
    compose groups(fizzbuzz)
}

def counts = pipe each(categories) {
    from it \
    count() \
    map context(it)
}

def statistics = pipe {
    from concat(counts) \
    doOnNext stats
}

drain statistics