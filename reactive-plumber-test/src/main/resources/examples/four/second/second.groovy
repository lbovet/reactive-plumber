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

join even to third.strings
join odd to third.strings
