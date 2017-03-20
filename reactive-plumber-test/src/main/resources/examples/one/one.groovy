package examples.one

import static Tools.*

def data = pipe {
    parallel from(Tools.input) \
    doOnNext show() \
    map wrap
}

def printer = {
    from(it) doOnNext show()
}

def renderer = pipe {
    from(data) \
    compose parallelize \
    map Tools.renderThread
}

def count = pipe {
    parallel from(data) count()
}

def size = pipe {
    from(data) \
    compose attach(count) \
    map Tools.renderSize \
    compose printer
}

def thread = pipe {
    from renderer compose printer
}

drain thread, size