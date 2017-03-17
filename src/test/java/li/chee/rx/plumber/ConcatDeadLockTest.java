package li.chee.rx.plumber;

import io.reactivex.Flowable;
import io.reactivex.Scheduler;
import io.reactivex.flowables.ConnectableFlowable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import org.junit.Ignore;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;

import static io.reactivex.Flowable.*;

/**
 * Created by bovetl on 17.03.2017.
 */
public class ConcatDeadLockTest {
    @Test
    public void testConcatDeadlock() throws InterruptedException {
        Scheduler comp = Schedulers.computation();
        ConnectableFlowable<String> a = just("1", "2", "3").doOnNext(log("a")).publish();
        Flowable<String> b = a.observeOn(comp).map(x -> x+" b").share();
        Flowable<String> c = a.observeOn(comp).map(x -> x+" c").doOnNext(log("c")).cache();
        Flowable<String> d = concat(b, c).observeOn(comp).doOnNext(log("D")).share();

        c.subscribe();
        CountDownLatch latch = new CountDownLatch(1);
        d.last("").subscribe( x -> latch.countDown() );
        a.connect();

        latch.await();
    }

    private static Consumer<String> log(String logger) {
        return  (t) -> System.out.println(logger+": "+t);
    }
}
