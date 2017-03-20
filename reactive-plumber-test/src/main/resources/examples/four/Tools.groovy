package examples.four

import li.chee.reactive.plumber.Box
import li.chee.reactive.plumber.Plumbing

import java.util.function.Function

abstract class Tools extends Plumbing {

    static input = range(1, 15).map((Function)Box.&wrap)

    static enum FizzBuzz { NONE, FIZZ, BUZZ, FIZZBUZZ }

    static fizzbuzz = { Box box ->
        def fizz = box.getValue() % 3 == 0
        def buzz = box.getValue() % 5 == 0
        def both = fizz && buzz
        both ? FizzBuzz.FIZZBUZZ: fizz ? FizzBuzz.FIZZ : buzz ? FizzBuzz.BUZZ : FizzBuzz.NONE
    }

    static stats = { Box box ->
        if(box.getContext(FizzBuzz.class) != FizzBuzz.NONE) {
            println box.getContext(FizzBuzz.class).toString() + ": " + box.getValue()
        }
    }

}
