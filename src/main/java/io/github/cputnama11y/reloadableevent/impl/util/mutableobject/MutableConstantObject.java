package io.github.cputnama11y.reloadableevent.impl.util.mutableobject;

import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

public sealed interface MutableConstantObject<T> extends Consumer<T>, Supplier<T> permits MutableConstantObjectImpl {
    default void modify(UnaryOperator<T> modifier) {
        accept(modifier.apply(get()));
    }

    public static <T> MutableConstantObject<T> create() {
        return new MutableConstantObjectImpl<>();
    }

    public static <T> MutableConstantObject<T> create(T initialValue) {
        return new MutableConstantObjectImpl<>(initialValue);
    }
}
