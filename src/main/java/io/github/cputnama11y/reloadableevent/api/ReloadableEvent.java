package io.github.cputnama11y.reloadableevent.api;

import com.mojang.serialization.MapCodec;
import io.github.cputnama11y.reloadableevent.impl.ReloadableEventImpl;
import net.minecraft.resources.Identifier;

import java.util.function.Function;

public interface ReloadableEvent<T extends ReloadableEvent.MapTyped<T>> {
    T invoker();

    void registerType(Identifier id, MapCodec<? extends T> type);

    static <T extends MapTyped<T>> ReloadableEvent<T> createAndRegister(Identifier id, Class<T> eventClass, Function<T[], T> invokerFactory) {
        return ReloadableEventImpl.createAndRegister(id, eventClass, invokerFactory);
    }

    static <T extends MapTyped<T>> ReloadableEvent<T> createEarlyReloadAndRegister(Identifier id, Class<T> eventClass, Function<T[], T> invokerFactory) {
        return ReloadableEventImpl.createEarlyReloadAndRegister(id, eventClass, invokerFactory);
    }

    @SuppressWarnings("unchecked")
    static <T extends MapTyped<T>> MapCodec<T> invokerType() {
        return (MapCodec<T>) ReloadableEventImpl.INVOKER_TYPE;
    }

    interface MapTyped<T extends MapTyped<T>> {
        MapCodec<? extends T> type();
    }
}
