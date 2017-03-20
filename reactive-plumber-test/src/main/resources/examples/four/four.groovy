package examples.four

import static Tools.*

def categories = pipe {
    from Tools.input \
    transform groups(Tools.fizzbuzz)
}

def counts = pipe each(categories) {
    from it \
    count() \
    map context(it)
}

def statistics = pipe {
    from concat(counts) \
    doOnNext Tools.stats
}

drain statistics