package io.github.cputnama11y.reloadableevent.api;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.cputnama11y.reloadableevent.impl.ReloadableEventImpl;
import net.fabricmc.fabric.api.event.Event;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.MultiRegistryBootstrap;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Util;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

public interface ReloadableEvent<T extends ReloadableEvent.MapTyped<T>> {
    @Nullable T invoker();

    @Nullable T pendingInvoker();

    void register(Identifier id, MapCodec<? extends T> type);

    MultiRegistryBootstrap bootstrap(Bootstrap<T> bootstrap);

    static <T extends MapTyped<T>> ReloadableEvent<T> createAndRegister(Identifier id, Class<T> eventClass, Function<T[], T> invokerFactory) {
        return ReloadableEventImpl.createAndRegister(id, eventClass, invokerFactory);
    }

    static <T extends MapTyped<T>> ReloadableEvent<T> createAndRegister(Identifier id, Class<T> eventClass, Function<T[], T> invokerFactory, Consumer<ReloadableEvent<T>> defaultBootstrap) {
        return Util.make(ReloadableEventImpl.createAndRegister(id, eventClass, invokerFactory), defaultBootstrap);
    }

    @SuppressWarnings("unchecked")
    static <T extends MapTyped<T>> MapCodec<T> unencodable() {
        return (MapCodec<T>) ReloadableEventImpl.UNENCODABLE_TYPE;
    }

    interface MapTyped<T extends MapTyped<T>> {
        default MapCodec<? extends T> type() {
            return unencodable();
        }
    }

    interface Bootstrap<T> {
        MultiRegistryBootstrap create(ResourceKey<Registry<Phased<T>>> callbacks, ResourceKey<Registry<PhaseData>> phases);
    }

    record Phased<T>(T callback, Identifier phase) {
        public static <T> MapCodec<Phased<T>> codec(MapCodec<T> codec) {
            return RecordCodecBuilder.mapCodec(instance -> instance.group(
                    codec.forGetter(Phased::callback),
                    Identifier.CODEC.optionalFieldOf("phase", Event.DEFAULT_PHASE).forGetter(Phased::phase)
            ).apply(instance, Phased::new));
        }
    }

    record PhaseData(List<Identifier> before, List<Identifier> after) {
        public static final Codec<PhaseData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Identifier.CODEC.listOf().fieldOf("before").forGetter(PhaseData::before),
                Identifier.CODEC.listOf().fieldOf("after").forGetter(PhaseData::after)
        ).apply(instance, PhaseData::new));
    }
}
