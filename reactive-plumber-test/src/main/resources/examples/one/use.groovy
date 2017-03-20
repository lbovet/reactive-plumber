package examples.one

import static Tools.*

Pipe a
Block printer

drain pipe {
    from a \
    compose attach(a) \
    to printer
}


