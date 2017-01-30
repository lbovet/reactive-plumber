package examples.two

import io.reactivex.Flowable

import static Tools.*

def marked = pipe (input) {
    from it \
    map parity
}

def even = pipe (marked) {
    from it \
    filter context(Parity.EVEN) \
    count()
}

def odd = pipe (marked) {
    from it \
    filter context(Parity.ODD) \
    count()
}

sink( Flowable.concat(even, odd)) {
    from it doOnNext print
}

done()
