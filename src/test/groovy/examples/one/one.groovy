package examples.one

import static Tools.*

def data = pipe {
    from input map wrap
}

def renderer = pipe {
    parallel from(data) map renderThread
}

def count = pipe {
    from data count()
}

def printer = {
    from it doOnNext print
}

sink \
pipe {
    from data \
    compose attach(count) \
    map renderSize \
    to printer
}, pipe {
    from renderer to printer
}

