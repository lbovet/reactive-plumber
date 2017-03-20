package examples.five

import io.reactivex.Flowable
import li.chee.rx.plumber.RxPlumbing

abstract class Tools extends RxPlumbing {
    static input = Flowable.just("hello", 5, 3, "world")
    static types = [
            { it instanceof String },
            { it instanceof Integer }
    ]
    static line = { x, y -> x + " " + y }
}
