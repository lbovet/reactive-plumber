package li.chee.rx.plumber;

import io.reactivex.Flowable;
import io.reactivex.flowables.ConnectableFlowable;
import io.reactivex.functions.Consumer;
import org.junit.Ignore;
import org.junit.Test;

import static io.reactivex.Flowable.*;

/**
 * Created by bovetl on 17.03.2017.
 */
public class ConcatDeadLockTest {
    @Test
    public void testConcatDeadlock() {
        Flowable a = just(1, 2, 3).doOnNext(log("a")).publish().refCount();
        Flowable b = a.map(x -> x+" b").doOnNext(log("b"));
        Flowable c = a.map(x -> x+" c").doOnNext(log("c"));
        Flowable d = concat(c, b).doOnNext(log("D"));

        d.subscribe();
        //b.connect();
        //c.connect();
        //a.connect();
    }

    private static <T> Consumer<T> log(String logger) {
        return  (T t) -> System.out.println(logger+": "+t);
    }
}
