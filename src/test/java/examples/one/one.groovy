package examples.one

import static Tools.*

def data = pipe (input) {
    from it map wrap
}

def renderer = pipe (data) {
    parallel from(it) map renderThread
}

def count = pipe (data) {
    from it count()
}

def printer = {
    from it doOnNext print
}

sink (data) {
    from it \
    compose attach(count) \
    map renderSize \
    to printer
}

sink (renderer) {
    from it to printer
}

done()
