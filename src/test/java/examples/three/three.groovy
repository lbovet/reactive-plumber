package examples.three

import static Tools.*

def marked = pipe (input) {
    from it \
    map parity
}

sink (marked) {
    from it \
    filter context(Parity.EVEN) \
    count() \
    doOnSuccess print
}

sink (marked) {
    from it \
    filter context(Parity.ODD) \
    count() \
    doOnSuccess print
}

done()
