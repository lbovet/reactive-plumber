package ten

import static li.chee.reactive.plumber.Plumbing.*

def p = tube {
    from range(0, 300) \
    doOnNext show() \
    compose fork() \
    doOnNext { throw new IllegalStateException() } \
    hide() \
    doOnError { e -> throw new IllegalArgumentException(e) } \
    compose fork() \
    doOnNext show("again") \
}

try {
    drain p
} catch(Throwable t) {
    println "caught"
    t.printStackTrace()
}
