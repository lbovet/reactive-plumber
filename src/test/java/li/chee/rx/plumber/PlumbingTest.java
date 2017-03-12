package li.chee.rx.plumber;

import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests the plumbing tools using groovy scripts.
 */
public class PlumbingTest {

    @Test
    public void testSimplePipeWork() {
        List<Object> events = new SimplePipeWork().execute().events();
        assertThat(events, contains(1, 2, 3, "hello"));
    }

    @Test
    public void testRuntimeOne() throws IOException {
        Object result = new Runtime(true).run(new String(Files.readAllBytes(Paths.get("src/test/groovy/examples/one/one.groovy"))));
        assertEquals('[', result.toString().charAt(0));
    }

    @Test
    public void testRuntimeTwo() throws IOException {
        Object result = new Runtime(true).run(new String(Files.readAllBytes(Paths.get("src/test/groovy/examples/two/two.groovy"))));
        assertEquals('[', result.toString().charAt(0));
    }

    @Test
    public void testRuntimeThree() throws IOException {
        Object result = new Runtime(true).run(new String(Files.readAllBytes(Paths.get("src/test/groovy/examples/three/three.groovy"))));
        assertEquals('[', result.toString().charAt(0));
    }

    @Test
    public void testRuntimeFour() throws IOException {
        Object result = new Runtime(true).run(new String(Files.readAllBytes(Paths.get("src/test/groovy/examples/four/four.groovy"))));
        assertEquals('[', result.toString().charAt(0));
    }

    @Test
    public void testRuntimeFive() throws IOException {
        Object result = new Runtime(true).run(new String(Files.readAllBytes(Paths.get("src/test/groovy/examples/five/five.groovy"))));
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

}
