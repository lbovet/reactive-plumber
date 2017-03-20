package li.chee.reactive.plumber;

import java.util.function.BiFunction;
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
        if(contextHolder != null) {
            return contextHolder.get(clazz);
        } else {
            throw new IllegalStateException("No context defined for this box");
        }
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

    public static <V> Box<V> wrap(V value) {
        if(value instanceof Box) {
            @SuppressWarnings("unchecked") Box<V> result = (Box<V>)value;
            return result;
        } else {
            return new Box<>(value);
        }
    }

    public static <V,C> Box<V> attach(Box<V> box, C context) {
        return box.with(context);
    }

    public <U> Box<U> map(Function<T, U> f) {
        return copy(f.apply(getValue()));
    }

    public static <U,V> Function<Box<U>, Box<V>> mapper(final Function<U, V> f) {
        return box -> box.map(f);
    }

    public <U> Box<U> flatMap(Function<T, Box<U>> f) {
        Box<U> added = f.apply(getValue());
        if(added.contextHolder != null) {
            if(this.contextHolder != null) {
                added.contextHolder.append(this.contextHolder);
            }
        } else {
            if(this.contextHolder != null) {
                added.contextHolder = this.contextHolder;
            }
        }
        return added;
    }

    public static <U,V> Function<Box<U>, Box<V>> binder(final Function<U, Box<V>> f) {
        return box -> box.flatMap(f);
    }

    public static <V> V unwrap(Box<V> box) {
        return box.getValue();
    }

    /**
     * A recursive context holder.
     */
    private static class ContextHolder<T> {

        private T value;
        private ContextHolder<?> inner;

        ContextHolder(T value) {
            this.value = value;
        }

        ContextHolder(T value, ContextHolder inner) {
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

        void append(ContextHolder<?> contextHolder) {
            if(inner == null) {
                inner = contextHolder;
            } else {
                inner.append(contextHolder);
            }
        }

        public String toString() {
            String result = value.toString();
            if(inner != null) {
                result = result + "," + inner.toString();
            }
            return result;
        }
    }

    @Override
    public String toString() {
        return "["+item.toString()+"|"+
                (contextHolder != null? contextHolder.toString() : "")+"]";
    }
}
