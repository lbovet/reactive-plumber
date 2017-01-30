package examples.one

import li.chee.rx.plumber.Box
import li.chee.rx.plumber.Plumbing

class Tools extends Plumbing {
    static input = Domain.&input
    static print = Domain.&print
    static render = Domain.&render
    static wrap = Box.&wrap
    static context = Box.&context
}
