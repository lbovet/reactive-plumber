package li.chee.rx.plumber;

import rx.functions.Func1;

import java.util.function.Function;

/**
 * An object wrapper with extensible context.
 */
public class Box<T> {

    private T item;
    private ContextHolder<?> contextHolder;

    public Box(T item) {
        this.item = item;
    }

    private Box(T item, ContextHolder<?> contextHolder) {
        this.contextHolder = contextHolder;
        this.item = item;
    }

    public T getValue() {
        return item;
    }

    public <V> V getContext(Class<? extends V> clazz) {
        return contextHolder.get(clazz);
    }

    public <C> Box<T> with(C context) {
        if(this.contextHolder == null) {
            return new Box<>(item, new ContextHolder<>(context));
        } else {
            return new Box<>(item, new ContextHolder<>(context, this.contextHolder));
        }
    }

    public <V> Box<V> copy(V value) {
        return new Box<>(value, this.contextHolder);
    }

    public static <V> Func1<Box<V>, Box<V>> context(Object context) {
        return (Box<V> b) -> b.with(context);
    }

    public static <V> Box<V> wrap(V value) {
        if(value instanceof Box) {
            @SuppressWarnings("unchecked") Box<V> result = (Box<V>)value;
            return result;
        } else {
            return new Box<>(value);
        }
    }

    /**
     * A recursive context holder.
     */
    private static class ContextHolder<T> {

        private T value;
        private ContextHolder<?> inner;

        public ContextHolder(T value) {
            this.value = value;
        }

        public ContextHolder(T value, ContextHolder inner) {
            this.value = value;
            this.inner = inner;
        }

        <V> V get(Class<? extends V> clazz) {
            if(clazz.isAssignableFrom(value.getClass())) {
                @SuppressWarnings("unchecked") V result = (V)value;
                return result;
            } else if(inner !=null) {
                return inner.get(clazz);
            } else {
                throw new IllegalStateException("There is no  "+clazz.getSimpleName()+" in the context chain");
            }
        }
    }
}
