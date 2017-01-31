package examples.five

import static Tools.*

def data = pipe {
    from input
}

def (strings, numbers) = split(types, data)

sink pipe {
    from strings \
    reduce("strings",line) \
    doOnSuccess print
}, pipe {
    from numbers \
    reduce("numbers",line) \
    doOnSuccess print
}



