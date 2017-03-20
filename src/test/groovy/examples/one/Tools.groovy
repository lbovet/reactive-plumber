package examples.one

import io.reactivex.Flowable
import io.reactivex.functions.Function
import li.chee.rx.plumber.RxPlumbing

abstract class Tools extends RxPlumbing {
    static input = Domain.&input
    static renderThread = Domain.&renderThread
    static renderSize = Domain.&renderSize

    static abstract class Pipe extends Flowable {}
    static abstract interface Block extends Function {}
}
