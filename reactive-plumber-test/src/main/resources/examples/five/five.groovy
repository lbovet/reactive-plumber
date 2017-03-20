package examples.five

import static Tools.*

def data = pipe {
    from Tools.input
}

def (strings, numbers) = split(Tools.types, data)

def stringPrint = pipe {
    from strings \
    reduce("strings:", Tools.line) \
    doOnSuccess show()
}

def numberPrint = pipe {
    from numbers \
    reduce("numbers:", Tools.line) \
    doOnSuccess show()
}

drain stringPrint, numberPrint



