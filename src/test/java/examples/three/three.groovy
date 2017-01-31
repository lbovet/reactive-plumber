package examples.three

import static Tools.*

def numbers = pipe {
    from input \
    map fizzbuzz
}

def groups = pipe {
    from numbers \
    groupBy context
}

def counts = pipe all(groups) {
    from it \
    count() \
    map with(key(it))
}

sink \
pipe {
    from concat(counts) \
    doOnNext stats
},
pipe {
    from numbers \
    doOnNext print \
    doOnComplete { println "---"}
}