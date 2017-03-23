package li.chee.reactive.plumber;

/**
 * Created by shaman on 20/03/17.
 */
public class BoxStreamTest {

    /*
    @Test
    public void testBoxStream() {
        Flowable.just(1, 2)
                .map(Box::wrap)
                .map(b -> b.with("hello"))
                .map(b -> b.copy(b.getValue() + 1))
                .map(b -> b.getContext(String.class) + " " + b.getValue())
                .test()
                .assertValues("hello 2", "hello 3");

        Flowable.just(1, 2)
                .map(Box::wrap)
                .map(mapper(x -> x+1))
                .map(binder(x -> wrap(x+1).with("hello")))
                .map(b -> b.getContext(String.class) + " " + b.getValue())
                .test()
                .assertValues("hello 3", "hello 4");


        Flowable.just(1, 2)
                .map(Box::wrap)
                .map(Box::wrap)
                .compose(Box.attach(Flowable.just("hello")))
                .map(b -> b.getContext(String.class) + " " + b.getValue())
                .test()
                .assertValues("hello 1", "hello 2");


    }*/
}
