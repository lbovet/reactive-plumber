package examples.two

import Box
import li.chee.reactive.plumber.Plumbing

abstract class Tools extends Plumbing {
    static input = range(1, 5).map Box.&wrap
    static parity = { Box box -> box.with(box.getValue() % 2 ? Parity.ODD : Parity.EVEN) }
    static context(value){
        { box -> box.getContext(value.getClass()) == value }
    }
    static with(value) {
        { it -> new Box(it).with(value) }
    }
    static enum Parity { EVEN, ODD }
    static EVEN = Parity.EVEN
    static ODD = Parity.ODD
}
