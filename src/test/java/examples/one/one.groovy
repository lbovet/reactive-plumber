package examples.one

import static Tools.*

def data = pipe {
    from input \
    map wrap \
    map context("hello")
}

def renderer = pipe {
    parallel from(data) \
    map render \
}

def count = pipe {
    from renderer \
    count()
}

sink {
    from renderer \
    concatWith count \
    doOnNext print
}

sink {
    from renderer \
    doOnNext { System.out.println "hello" }
}

done()
