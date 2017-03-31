package examples.nine

import static li.chee.reactive.plumber.Plumbing.*

fromIterable([range(0, 300).toIterable()])
        .flatMap( { fromIterable(it) } )
        .share()
        .log()
        .share()
        .subscribe()