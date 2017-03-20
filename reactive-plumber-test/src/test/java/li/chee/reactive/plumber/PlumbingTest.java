package li.chee.reactive.plumber;

import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.Stream;

import static li.chee.reactive.plumber.Box.binder;
import static li.chee.reactive.plumber.Box.mapper;
import static li.chee.reactive.plumber.Box.wrap;
import static org.junit.Assert.assertTrue;

/**
 * Tests the plumbing tools using groovy scripts.
 */
public class PlumbingTest {

    @Test
    public void testRuntimeOne() throws IOException {
        new Runtime(true).withGraphShowToLinks(true).run(new String(Files.readAllBytes(Paths.get("src/test/groovy/examples/one/one.groovy"))));
    }

    @Test
    public void testRuntimeTwo() throws IOException {
        new Runtime(true).run(new String(Files.readAllBytes(Paths.get("src/test/groovy/examples/two/two.groovy"))));
    }

    @Test
    public void testRuntimeThree() throws IOException {
        new Runtime(true).run(new String(Files.readAllBytes(Paths.get("src/test/groovy/examples/three/three.groovy"))));
    }

    @Test
    public void testRuntimeFour() throws IOException {
        new Runtime(true).run(new String(Files.readAllBytes(Paths.get("src/test/groovy/examples/four/four.groovy"))));
    }

    @Test
    public void testRuntimeFive() throws IOException {
        new Runtime(true).run(new String(Files.readAllBytes(Paths.get("src/test/groovy/examples/five/five.groovy"))));
    }

    @Test
    public void testGeneratePngOutputStream() throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        new Runtime().generateGraph
                (new String(Files.readAllBytes(Paths.get("src/test/groovy/examples/one/one.groovy"))),
                        "png",
                        bos);
        assertTrue(bos.size() > 0);
        assertTrue(new String(bos.toByteArray()).substring(0, 10).contains("PNG"));
    }

    @Test
    public void testGenerateSvgOutputStream() throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        new Runtime().generateGraph
                (new String(Files.readAllBytes(Paths.get("src/test/groovy/examples/one/one.groovy"))),
                        "svg",
                        bos);
        assertTrue(bos.size() > 0);
        assertTrue(new String(bos.toByteArray()).substring(0, 120).contains("svg"));
    }

    @Test
    public void testGenerateGifFile() throws IOException {
        File f = new File("target/graph.gif");
        new Runtime().generateGraph(new String(Files.readAllBytes(Paths.get("src/test/groovy/examples/one/one.groovy"))), f);
        String s = new String(Files.readAllBytes(f.toPath())).substring(0, 10);
        assertTrue(s.contains("GIF"));
        f.delete();
        f.deleteOnExit();
    }

    @Test
    public void generateAllImages() throws IOException {
        Stream.of("one", "two", "three", "four", "five").forEach(script -> {
                    try {
                        new Runtime().withGraphTheme(Runtime.GraphTheme.LIGHT).generateGraph(new String(Files.readAllBytes(Paths.get("src/test/groovy/examples/" + script + "/" + script + ".groovy"))), new File("target/"+script+".png"));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
        );
    }

    /*
    @Test
    public void testStream() {
        Plumbing.just(1, 2)
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
