package examples.context
import li.chee.rx.plumber.Plumbing
import io.reactivex.Flowable

class Domain extends Plumbing {
    static input = Flowable.range(1, 4)
    static print = { System.out.println it }
    static render = { it.getContext(String.class) + " " + Thread.currentThread().getName() + " " + it.getValue() }
}
