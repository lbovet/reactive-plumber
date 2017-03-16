package examples.two

import static Tools.*

def marked = pipe {
    from input \
    doOnNext show \
    map parity \
}

def even = pipe {
    from marked \
    filter context(EVEN) \
    doOnNext show \
    count() \
    map with("=>" + EVEN) \
    doOnSuccess show
}
/*
def odd = pipe {
    from marked \
    filter context(ODD)\
    doOnNext show \
    count() \
    map with(ODD) \
    doOnSuccess show
}*/

def inc = pipe {
    from even \
    map mapper { it+1} \
    doOnNext show \
}

def dec = pipe {
    from even \
    map mapper { it-1} \
    doOnNext show \
}

drain inc, dec

