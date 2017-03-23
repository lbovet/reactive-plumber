package examples.one

import static One.*

def data = pipe {
    concurrent from(input) \
    map wrap
}

def printer = {
    from it \
    doOnNext show()
}

def renderer = pipe {
    parallel from(data) \
    map renderThread
}

def count = pipe {
    from(data) \
    count()
}

def size = pipe {
    from(data) \
    zipWith value(count), attach \
    map renderSize \
    compose printer
}

def thread = pipe {
    from renderer compose printer
}

drain thread, size