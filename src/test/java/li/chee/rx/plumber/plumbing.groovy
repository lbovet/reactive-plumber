package li.chee.rx.plumber

import io.reactivex.Flowable
import io.reactivex.Single

input = Flowable.fromArray(1, 2, 3, 4)
def wrap = Box.&wrap
def context = Box.&context
def print = { System.out.println it }
def render = { it.getContext(String.class) + " " + it.getValue() }

def Flowable read(it) { it }

pipes = []

def start() {
    pipes.reverse().each { it.connect() }
}

def Flowable pipe(it) {
    result = it()
    if (Flowable.isAssignableFrom(result.getClass())) {
        result = result.publish()
    } else if (Single.isAssignableFrom(result.getClass())) {
        result = result.toFlowable().publish()
    } else {
        return null
    }
    pipes.add result
    result
}

// ---------------------

def data = pipe {
    read input  \
    map wrap  \
    map context("hello")  \
    map render
}

def count = pipe {
    read data  \
    count()
}

pipe {
    read data  \
    concatWith count  \
    subscribe print
}

// ----------------------

start()