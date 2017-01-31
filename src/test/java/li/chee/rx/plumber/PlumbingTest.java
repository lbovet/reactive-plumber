package li.chee.rx.plumber;

import org.junit.Test;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;

/**
 * Tests the plumbing tools using groovy scripts.
 */
public class PlumbingTest {

    @Test
    public void testSimplePipeWork() {
        List<Object> events = new SimplePipeWork().execute().events();
        assertThat(events, contains(1, 2, 3, "hello"));
    }
}
