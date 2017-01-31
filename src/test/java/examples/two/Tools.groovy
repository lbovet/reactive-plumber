package examples.two

import io.reactivex.Flowable
import li.chee.rx.plumber.Box
import li.chee.rx.plumber.Plumbing

abstract class Tools extends Plumbing {
    static input = Flowable.range(1, 5).map Box.&wrap
    static print = { println it.getValue() + " " + it.getContext(Parity.class)}
    static parity = { Box box -> box.with(box.getValue() % 2 ? Parity.ODD : Parity.EVEN) }
    def static context(value){
        { box -> box.getContext(value.getClass()) == value }
    }
    def static with(value) {
        { it -> new Box(it).with(value) }
    }
    static enum Parity { EVEN, ODD }
    static EVEN = Parity.EVEN
    static ODD = Parity.ODD
}
