package li.chee.reactive.plumber;

import org.junit.Test;

import java.util.function.Function;

import static org.junit.Assert.*;
import static li.chee.reactive.plumber.Box.*;

public class BoxTest {

    @Test
    public void testValue() {
        Box<String> box = new Box<>("hello");
        assertEquals("hello", box.getValue());
        assertEquals("hello", box.getValue());
        assertEquals("hello", unwrap(box));
        assertEquals("[hello|]", box.toString());
    }

    @Test
    public void testDifferentContexts() {
        Box<String> box = new Box<>("hello");
        box = box.with("context");
        box = box.with(12L);
        assertEquals("context", box.getContext(String.class));
        assertEquals((Long) 12L, box.getContext(Long.class));
        assertEquals("[hello|12,context]", box.toString());
    }

    @Test
    public void testContextOverride() {
        Box<String> box = new Box<>("hello");
        box = box.with("context");
        Box<String> box2 = box.with("context2");
        assertEquals("context", box.getContext(String.class));
        assertEquals("context2", box2.getContext(String.class));
    }

    @Test
    public void testCopy() {
        Box<String> box = new Box<>("hello");
        box = box.with("context");
        Box<Integer> box2 = box.copy(12);
        assertEquals("context", box2.getContext(String.class));
    }

    @Test(expected = IllegalStateException.class)
    public void testSimpleBoxWithoutContext() {
        Box<String> box = new Box<>("hello");
        box.getContext(String.class);
    }

    @Test(expected = IllegalStateException.class)
    public void testUnknownContext() {
        Box<String> box = new Box<>("hello");
        box = box.with("context");
        box.getContext(Integer.class);
    }

    private static boolean equals(Box a, Box b) {
        return a.getValue().equals(b.getValue()) &&
                a.getContext(String.class) == b.getContext(String.class) &&
                a.getContext(Integer.class) == b.getContext(Integer.class);
    }

    @Test
    public void testMonadicLaws() throws Exception {
        Function<String, Box<String>> f = (s -> wrap(s.toUpperCase()).with("foo").with(1));
        Function<String, Box<String>> g = (s -> wrap(s.toLowerCase()).with("bar").with(2));

        Box<String> b1 = wrap("Hi").with("bar").with(1).flatMap(Box::wrap);
        Box<String> b2 = wrap("Hi").with("bar").with(1);
        assertTrue(equals(b1, b2));

        b1 = wrap("hi").flatMap(f).flatMap(g);
        b2 = wrap("hi").flatMap(s -> f.apply(s).flatMap(g));
        assertTrue(equals(b1, b2));
    }
}
