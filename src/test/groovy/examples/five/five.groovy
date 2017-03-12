package examples.five

import static Tools.*

def data = pipe {
    from input
}

def (strings, numbers) = split(types, data)

def stringPrint = pipe {
    from strings \
    reduce("strings", line) \
    doOnSuccess print
}

def numberPrint = pipe {
    from numbers \
    reduce("numbers", line) \
    doOnSuccess print
}

sink stringPrint, numberPrint



