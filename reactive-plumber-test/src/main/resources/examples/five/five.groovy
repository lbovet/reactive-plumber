package examples.five

import static Five.*

def data = pipe {
    from input
}

def (strings, numbers) = split(types, data)

def stringPrint = pipe {
    from strings \
    cast String.class \
    reduce("messages:", line) \
    doOnSuccess show()
}

def numberPrint = pipe {
    from numbers \
    cast Integer.class \
    map Integer.&toString \
    reduce("length:", line) \
    doOnSuccess show()
}

drain stringPrint, numberPrint



