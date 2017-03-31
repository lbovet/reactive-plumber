package li.chee.reactive.plumber;

import examples.four.first.First;
import examples.four.second.Second;
import org.junit.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertArrayEquals;
import static reactor.core.publisher.Flux.just;

/**
 * Tests the plumbing tools using groovy scripts.
 */
public class GenericPlumbingTest {

    private static String ROOT = "../reactive-plumber-test/src/main/resources/examples";

    @Test
    public void testRuntimeOne() throws IOException {
        new Runtime(true).withGraphShowToLinks(true).run(new String(Files.readAllBytes(Paths.get(ROOT+"/one/one.groovy"))));
    }

    @Test
    public void testRuntimeTwo() throws IOException {
        new Runtime(true).run(new String(Files.readAllBytes(Paths.get(ROOT+"/two/two.groovy"))));
    }

    @Test
    public void testRuntimeThree() throws IOException {
        new Runtime(true).run(new String(Files.readAllBytes(Paths.get(ROOT+"/three/three.groovy"))));
    }

    @Test
    public void testRuntimeFour() throws IOException, URISyntaxException {
        new Runtime(true)
                .run(First.class.getResource("first.groovy").toURI())
                .run(Second.class.getResource("second.groovy").toURI())
                .generateOverviewGraph();
    }

    @Test
    public void testRuntimeFourFirst() throws IOException, URISyntaxException {
        Runtime r = new Runtime(true);
        r.run(First.class.getResource("first.groovy").toURI());
        List<Integer> numbers = new ArrayList<>();
        First.drain(First.exports.even.doOnNext(numbers::add));
        assertArrayEquals(new Integer[]{ 2, 4}, numbers.toArray());
    }

    @Test
    public void testRuntimeFourSecond() throws IOException, URISyntaxException {
        Runtime r = new Runtime(true);
        First.exports.even = just(2,4,6);
        First.exports.odd = just(1,3,5);
        r.run(Second.class.getResource("second.groovy").toURI());
    }

    @Test
    public void testRuntimeFive() throws IOException {
        new Runtime(true).run(new String(Files.readAllBytes(Paths.get(ROOT+"/five/five.groovy"))));
    }

    @Test
    public void testRuntimeSix() throws IOException {
        new Runtime(true).run(new String(Files.readAllBytes(Paths.get(ROOT+"/six/six.groovy"))));
    }

    @Test
    public void testRuntimeSeven() throws IOException {
        new Runtime(true).run(new String(Files.readAllBytes(Paths.get(ROOT+"/seven/seven.groovy"))));
    }
}
