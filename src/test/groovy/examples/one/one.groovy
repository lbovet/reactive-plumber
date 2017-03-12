package examples.one

import static Tools.*

def data = pipe {
    from input map wrap
}

def printer = {
    from it doOnNext print
}

def renderer = pipe {
    parallel from(data) map renderThread
}

def count = pipe {
    from data count()
}

def size = pipe {
    from data \
    compose attach(count) \
    map renderSize \
    to printer
}

def thread = pipe {
    from renderer to printer
}

drain size, thread
