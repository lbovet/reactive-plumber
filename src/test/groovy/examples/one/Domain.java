package examples.one;

import io.reactivex.Flowable;
import li.chee.rx.plumber.Box;

/**
 * Example domain
 */
public class Domain {

    public static Flowable<Integer> input() {
        return Flowable.range(1, 4);
    }

    public static void print(Object it) {
        System.out.println(it);
    }

    public static <T> String render(Box<T> box) {
        return box.getContext(String.class) + " " +
                Thread.currentThread().getName() + " " + box.getValue();
    }
}
