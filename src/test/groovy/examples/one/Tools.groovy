package examples.one

import io.reactivex.Flowable
import io.reactivex.functions.Function
import li.chee.rx.plumber.Plumbing

abstract class Tools extends Plumbing {
    static input = Domain.&input
    static print = Domain.&print
    static renderThread = Domain.&renderThread
    static renderSize = Domain.&renderSize

    static abstract class Pipe extends Flowable {}
    static abstract interface Block extends Function {}
}
