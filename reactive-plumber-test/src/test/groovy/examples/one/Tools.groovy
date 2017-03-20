package examples.one


import li.chee.reactive.plumber.Plumbing

abstract class Tools extends Plumbing {
    static input = just(1, 2, 3, 4)
    static renderThread = Domain.&renderThread
    static renderSize = Domain.&renderSize
}
