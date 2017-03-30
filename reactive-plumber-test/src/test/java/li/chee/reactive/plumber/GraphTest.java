package li.chee.reactive.plumber;

import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.Stream;

import static org.junit.Assert.assertTrue;

public class GraphTest {

    private static String ROOT = "src/main/resources/examples";

    @Test
    public void testGeneratePngOutputStream() throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        new Runtime().generateGraph
                (new String(Files.readAllBytes(Paths.get(ROOT+"/one/one.groovy"))),
                        "png",
                        bos);
        assertTrue(bos.size() > 0);
        assertTrue(new String(bos.toByteArray()).substring(0, 10).contains("PNG"));
    }

    @Test
    public void testGenerateSvgOutputStream() throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        new Runtime().generateGraph
                (new String(Files.readAllBytes(Paths.get(ROOT+"/one/one.groovy"))),
                        "svg",
                        bos);
        assertTrue(bos.size() > 0);
        assertTrue(new String(bos.toByteArray()).substring(0, 120).contains("svg"));
    }

    @Test
    public void testGenerateGifFile() throws IOException {
        File f = new File("target/graph.gif");
        new Runtime().generateGraph(new String(Files.readAllBytes(Paths.get(ROOT+"/one/one.groovy"))), f);
        String s = new String(Files.readAllBytes(f.toPath())).substring(0, 10);
        assertTrue(s.contains("GIF"));
        f.delete();
        f.deleteOnExit();
    }

    @Test
    public void generateAllImages() throws IOException {
        Stream.of("one", "two", "three", "five", "six", "seven").forEach(script -> {
                    try {
                        new Runtime().withGraphTheme(Runtime.GraphTheme.LIGHT).generateGraph(new String(Files.readAllBytes(Paths.get(ROOT + "/" + script + "/" + script + ".groovy"))), new File("target/" + script + ".png"));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
        );
    }

    @Test
    public void generateExampleFourImages() throws IOException {
        new Runtime().withGraphTheme(Runtime.GraphTheme.LIGHT).generateGraph(new String(Files.readAllBytes(Paths.get(ROOT+"/four/first/first.groovy"))), new File("target/four-first.png"));
        new Runtime().withGraphTheme(Runtime.GraphTheme.LIGHT).generateGraph(new String(Files.readAllBytes(Paths.get(ROOT+"/four/second/second.groovy"))), new File("target/four-second.png"));
    }
}
