package li.chee.rx.plumber

import static Plumbing.*

class SimplePipeWork extends Recorder {
    void run() {
        def input = just(1, 2, 3)

        def p = pipe {
            from input \
            map wrap \
            compose attach(just("hello")) \
            doOnNext { println it } \
            doOnNext { Box b -> send(b.getValue()) } \
            lastOrError() \
            doOnSuccess { Box b -> send(b.getContext(String.class)) }
        }

        drain p
    }
}
