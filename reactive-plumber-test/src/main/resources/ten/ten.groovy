package ten

import static li.chee.reactive.plumber.Plumbing.*

def printVoters = pipe {
    from range(0, 300) \
    compose {f -> just(f.toIterable())} \
    flatMapIterable { it } \
    doOnNext show("print")
}

def end = pipe {
    from printVoters \
    doOnNext show("end")
}

drain end
