package examples.seven

import static Seven.*

def data = pipe {
    from input \
    doOnNext show("input")
}

def multiple = tube {
    from data \
    doOnNext show("multiple")
}

def left = pipe {
    from multiple \
    doOnNext show("left")
}

def right = pipe {
    from multiple \
    doOnNext show("right")
}

def all = pipe {
    from merge(left, right) \
    doOnNext show("merged")
}

drain all