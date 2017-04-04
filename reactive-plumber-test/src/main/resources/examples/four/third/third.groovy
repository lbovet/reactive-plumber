package examples.four.third

import static Third.*

def output = pipe {
    from merge(exports.strings) \
    doOnNext show()
}

drain output