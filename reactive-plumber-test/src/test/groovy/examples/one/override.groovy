package examples.one

import static Tools.*

// bindings.overrides or expando ?
def v = "input"
c = Tools.class
c[v] = { just 1, 2, 3 }

def a = pipe {
    from input \
    map { it * 2 }
}

def printer = {
    from it doOnNext { print it }
}

drain pipe { from a to printer }

// get name
export a, printer