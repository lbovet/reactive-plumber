package examples.four.second

import static Second.*

def even = pipe {
    from first.even \
    map toString \
    reduce "Even:", line
}

def odd = pipe {
    from first.odd \
    map toString \
    reduce "Odd:", line
}

def output = pipe {
    from merge(even, odd) \
    doOnNext show()
}

drain output