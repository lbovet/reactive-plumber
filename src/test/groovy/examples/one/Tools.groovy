package examples.one


import li.chee.rx.plumber.Plumbing
import reactor.core.publisher.Flux

import java.util.function.Function

abstract class Tools extends Plumbing {
    static input = just(1, 2, 3, 4)
    static renderThread = Domain.&renderThread
    static renderSize = Domain.&renderSize

    static abstract class Pipe extends Flux {}
    static abstract interface Block extends Function {}
}
