package examples.six

import static Tools.*

def data = pipe {
    from input \
    map process
}

def stringPrint = pipe {
    from data \
    flatMapIterable select(FIRST) \
    doOnNext show("string")
}

def numberPrint = pipe {
    from data \
    map select(SECOND) \
    doOnNext show("number")
}

drain stringPrint, numberPrint



