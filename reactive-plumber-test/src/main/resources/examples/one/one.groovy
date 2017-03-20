package examples.one

import static Tools.*

def data = pipe {
    from(input) \
    map wrap
}

def printer = {
    from(it) doOnNext show()
}

def renderer = pipe {
    from(data) \
    map renderThread
}

def count = pipe {
    from(data) count()
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