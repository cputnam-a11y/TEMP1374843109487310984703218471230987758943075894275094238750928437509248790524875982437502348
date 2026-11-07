package io.github.cputnama11y.reloadableevent.test;

import io.github.cputnama11y.reloadableevent.api.ReloadableEvent;
import net.minecraft.resources.Identifier;

public interface Listener extends ReloadableEvent.MapTyped<Listener> {
    ReloadableEvent<Listener> EVENT = ReloadableEvent.createAndRegister(
            Identifier.fromNamespaceAndPath("fabric", "demo"),
            Listener.class,
            listeners -> advice -> {
                for (Listener listener : listeners) {
                    listener.listenTo(advice);
                }
            },
            e -> e.register(Identifier.fromNamespaceAndPath("fabric", "speaker"), Speaker.CODEC)
    );


    void listenTo(String advice);
    public static void init() {

    }
}
