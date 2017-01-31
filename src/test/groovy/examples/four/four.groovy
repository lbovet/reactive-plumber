package examples.four

import static Tools.*

def categories = pipe {
    from input \
    to groups(fizzbuzz)
}

def counts = pipe all(categories) {
    from it \
    count() \
    map context(it)
}

sink pipe {
    from concat(counts) \
    doOnNext stats
}