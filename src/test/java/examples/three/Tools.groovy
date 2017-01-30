package examples.three

import io.reactivex.Flowable
import li.chee.rx.plumber.Box
import li.chee.rx.plumber.Plumbing

class Tools extends Plumbing {
    static input = Flowable.range(1, 5).map Box.&wrap
    static print = { println it }
    static parity = { Box box -> box.with(box.getValue() % 2 ? Parity.ODD : Parity.EVEN) }
    def static context(parity) {
         { it -> it.getContext(Parity.class) == parity }
    }
    static enum Parity {  EVEN, ODD }
}
