package li.chee.rx.plumber;

import org.junit.Test;

import static org.junit.Assert.*;

public class BoxTest {

    @Test
    public void testValue() {
        Box<String> box = new Box<>("hello");
        assertEquals("hello", box.getValue());
        box = box.with("context");
        assertEquals("hello", box.getValue());
    }

    @Test
    public void testDifferentContexts() {
        Box<String> box = new Box<>("hello");
        box = box.with("context");
        box = box.with(12L);
        assertEquals("context", box.getContext(String.class));
        assertEquals((Long)12L, box.getContext(Long.class));
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
    public void testIllegalSimpleBox() {
    }
}
