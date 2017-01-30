package examples.one

import li.chee.rx.plumber.Plumbing

class Tools extends Plumbing {
    static input = Domain.&input
    static print = Domain.&print
    static renderThread = Domain.&renderThread
    static renderSize = Domain.&renderSize
}
