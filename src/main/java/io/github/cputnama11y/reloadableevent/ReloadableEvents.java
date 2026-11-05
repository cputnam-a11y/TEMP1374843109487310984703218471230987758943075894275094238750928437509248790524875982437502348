package io.github.cputnama11y.reloadableevent;

import com.mojang.serialization.MapCodec;
import io.github.cputnama11y.reloadableevent.api.ReloadableEvent;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.resources.Identifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReloadableEvents implements ModInitializer {
    public static final String MOD_ID = "reloadableevent";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    interface Listener extends ReloadableEvent.MapTyped<Listener> {
        void listenTo(String advice);
    }

    static class Speaker implements Listener {
        public static final MapCodec<Speaker> CODEC = MapCodec.unit(Speaker::new);

        @Override
        public void listenTo(String boringStuff) {
            IO.println("Now Speaking: " + boringStuff);
        }

        @Override
        public MapCodec<? extends Listener> type() {
            return CODEC;
        }
    }

    @Override
    public void onInitialize() {
        var event = ReloadableEvent.createEarlyReloadAndRegister(
                Identifier.withDefaultNamespace("demo"),
                Listener.class,
                listeners -> new Listener() {
                    @Override
                    public void listenTo(String advice) {
                        for (Listener listener : listeners) {
                            listener.listenTo(advice);
                        }
                    }

                    @Override
                    public MapCodec<? extends Listener> type() {
                        return ReloadableEvent.invokerType();
                    }
                }
        );

        event.registerType(Identifier.fromNamespaceAndPath("fabric", "speaker"), Speaker.CODEC);

        ServerLifecycleEvents.SERVER_STARTED.register(_ -> event.invoker().listenTo("The Odyssey"));
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
}
