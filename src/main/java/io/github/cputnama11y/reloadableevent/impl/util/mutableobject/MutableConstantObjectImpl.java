package io.github.cputnama11y.reloadableevent.impl.util.mutableobject;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MutableCallSite;

record MutableConstantObjectImpl<T>(MutableCallSite object) implements MutableConstantObject<T> {
    MutableConstantObjectImpl() {
        this(new MutableCallSite(MethodHandles.zero(Object.class)));
    }

    MutableConstantObjectImpl(T initialValue) {
        this(new MutableCallSite(MethodHandles.constant(Object.class, initialValue)));
    }

    @Override
    public void accept(T t) {
        object.setTarget(MethodHandles.constant(Object.class, t));
        MutableCallSite.syncAll(new MutableCallSite[]{object});
    }

    @Override
    @SuppressWarnings("unchecked")
    public T get() {
        try {
            // should not throw, ever
            return (T) object.getTarget().invokeExact();
        } catch (Throwable e) {
            throw new RuntimeException("Unexpected Error Retrieving MutableObject value", e);
        }
    }

}
