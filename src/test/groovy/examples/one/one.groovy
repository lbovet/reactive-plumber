package examples.one

import static Tools.*

def data = pipe {
    parallel from(input) \
    doOnNext show() \
    map wrap
}

def printer = {
    from(it) doOnNext show()
}

def renderer = pipe {
    from(data) \
    compose parallelize \
    map renderThread
}

def count = pipe {
    parallel from(data) count()
}

def size = pipe {
    from(data) \
    compose attach(count) \
    map renderSize \
    compose printer
}

def thread = pipe {
    from renderer compose printer
}

drain thread, size