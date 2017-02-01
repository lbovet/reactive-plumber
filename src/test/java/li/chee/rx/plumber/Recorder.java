package li.chee.rx.plumber;

import io.reactivex.functions.Consumer;

import java.util.*;

/**
 * Provides event recording to assert pipe work.
 */
public abstract class Recorder implements Runnable {

    private Map<String,List<Object>> map = Collections.synchronizedMap(new HashMap<>());

    public void send(Object event) throws Exception {
        send("", event);
    }

    public void send(String category, Object event) throws Exception {
        sender(category).accept(event);
    }

    public Consumer sender() {
        return sender("");
    }

    public Consumer sender(String category) {
        map.putIfAbsent(category, new ArrayList<>());
        return (Object event) -> {
            map.get(category).add(event);
        };
    }

    public List<Object> events() {
        return events("");
    }

    public List<Object> events(String category) {
        return map.get(category);
    }

    public Recorder execute() {
        run();
        return this;
    }
}
