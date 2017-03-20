package examples.five

import static Tools.*

def data = pipe {
    from input
}

def (strings, numbers) = split(types, data)

def stringPrint = pipe {
    from strings \
    reduce("strings:", line) \
    doOnSuccess show()
}

def numberPrint = pipe {
    from numbers \
    reduce("numbers:", line) \
    doOnSuccess show()
}

drain stringPrint, numberPrint



