package li.chee.reactive.plumber;

import org.junit.Ignore;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.Stream;

import static org.junit.Assert.assertTrue;

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
    public void testRuntimeFour() throws IOException {
        new Runtime(true).run(new String(Files.readAllBytes(Paths.get(ROOT+"/four/four.groovy"))));
    }

    @Test
    public void testRuntimeFive() throws IOException {
        new Runtime(true).run(new String(Files.readAllBytes(Paths.get(ROOT+"/five/five.groovy"))));
    }

    @Test
    public void testRuntimeSix() throws IOException {
        new Runtime(true).run(new String(Files.readAllBytes(Paths.get(ROOT+"/six/six.groovy"))));
    }
}
