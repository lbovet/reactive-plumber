package examples.four.first

import static First.*

def data = pipe {
    from input
}

def even = pipe {
    from data \
    filter even
}

def odd = pipe {
    from data \
    filter odd
}

exports.even = even
exports.odd = odd