package examples.five

import io.reactivex.Flowable
import li.chee.rx.plumber.Box
import li.chee.rx.plumber.Plumbing

abstract class Tools extends Plumbing {
    static input = Flowable.just("hello", 5, 3, "world")
    static types = [
            { it instanceof String },
            { it instanceof Integer }
    ]
    static line = { x, y -> x + " " + y }
}
