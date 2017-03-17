package examples.three

import li.chee.rx.plumber.Box
import li.chee.rx.plumber.RxPlumbing

abstract class Tools extends RxPlumbing {

    static input = range(1, 15).map Box.&wrap

    static display = { Box box ->
        def context = box.getContext(FizzBuzz.class)
        println context == FizzBuzz.NONE ? box.getValue() : context
    }

    static with(value) {
        { it -> new Box(it).with(value) }
    }

    static fizzbuzz = { Box box ->
        def fizz = box.getValue() % 3 == 0
        def buzz = box.getValue() % 5 == 0
        def both = fizz && buzz
        box.with( both ? FizzBuzz.FIZZBUZZ: fizz ? FizzBuzz.FIZZ : buzz ? FizzBuzz.BUZZ : FizzBuzz.NONE )
    }

    static context = { Box box -> box.getContext(FizzBuzz.class) }

    static stats = { Box box ->
        if(box.getContext(FizzBuzz.class) != FizzBuzz.NONE) {
            println box.getContext(FizzBuzz.class).toString() + ": " + box.getValue()
        }
    }

    static enum FizzBuzz { NONE, FIZZ, BUZZ, FIZZBUZZ }
}
