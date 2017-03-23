package examples.six

import static Six.*

def data = pipe {
    from input \
    map process
}

def stringPrint = pipe {
    from data \
    flatMapIterable messages \
    doOnNext show("message")
}

def numberPrint = pipe {
    from data \
    map length \
    doOnNext show("length")
}

drain stringPrint, numberPrint



