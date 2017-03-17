package examples.four

import io.reactivex.Flowable
import li.chee.rx.plumber.Box
import li.chee.rx.plumber.Plumbing

abstract class Tools extends Plumbing {

    static input = Flowable.range(1, 15).map Box.&wrap

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
