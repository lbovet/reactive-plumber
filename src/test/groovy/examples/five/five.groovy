package examples.five

import static Tools.*

def data = pipe {
    from input
}

def (strings, numbers) = split(types, data)

def stringPrint = pipe {
    from strings
}

def numberPrint = pipe {
    from numbers
}

def merge = pipe {
    from zip(stringPrint, numberPrint, fuse) \
    doOnNext show
}

drain merge



