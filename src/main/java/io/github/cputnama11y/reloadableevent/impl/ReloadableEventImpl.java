package io.github.cputnama11y.reloadableevent.impl;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import io.github.cputnama11y.reloadableevent.api.ReloadableEvent;
import io.github.cputnama11y.reloadableevent.impl.util.mutableobject.MutableConstantObject;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricDynamicRegistryProvider;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.fabricmc.fabric.api.event.registry.DynamicRegistries;
import net.fabricmc.fabric.api.event.registry.FabricRegistryBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.MultiRegistryBootstrap;
import net.minecraft.data.DataProvider;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Unit;
import org.apache.commons.lang3.mutable.MutableInt;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

public record ReloadableEventImpl<T extends ReloadableEvent.MapTyped<T>>(Identifier id, MutableConstantObject<Event<T>> event, Registry<MapCodec<? extends T>> types, ResourceKey<Registry<PhaseData>> phases, ResourceKey<Registry<Phased<T>>> callbacks, Class<T> eventClass, Function<T[], T> invokerFactory) implements ReloadableEvent<T> {
    public static final MapCodec<? extends MapTyped<?>> UNENCODABLE_TYPE = MapCodec.unit(Unit.INSTANCE).flatXmap(
            _ -> DataResult.error(() -> "Cannot deserialize invoker"),
            _ -> DataResult.error(() -> "Cannot serialize invoker")
    );

    public static final Logger LOGGER = LoggerFactory.getLogger("reloadable-events");

    public static final ResourceKey<Registry<ReloadableEventImpl<?>>> KEY = ResourceKey.createRegistryKey(
            Identifier.fromNamespaceAndPath("fabric", "reloadable_events")
    );

    public static final Registry<ReloadableEventImpl<?>> REGISTRY = FabricRegistryBuilder.create(KEY).buildAndRegister();

    public ReloadableEventImpl(Identifier id, Class<T> eventClass, Function<T[], T> invokerFactory) {
        this(
                id,
                MutableConstantObject.create(),
                FabricRegistryBuilder.<MapCodec<? extends T>>create(
                        ReloadableEventsHooks.makeReloadableEventKey(id, "callback_type")
                ).buildAndRegister(),
                ReloadableEventsHooks.makeReloadableEventKey(id, "event_phases"),
                ReloadableEventsHooks.makeReloadableEventKey(id, "reloadable_events"),
                eventClass,
                invokerFactory
        );

        Registry.register(this.types, Identifier.fromNamespaceAndPath("fabric", "invoker"), ReloadableEvent.unencodable());

        DynamicRegistries.registerReloadable(this.phases, PhaseData.CODEC);
        DynamicRegistries.registerReloadable(this.callbacks, Phased.<T>codec(this.types.byNameCodec().dispatchMap(
                MapTyped::type,
                Function.identity()
        )).codec());
    }

    @SuppressWarnings("unchecked")
    @Override
    public @Nullable T pendingInvoker() {
        if (ReloadableEventsHooks.PENDING_EVENTS.isBound()) {
            var event = ReloadableEventsHooks.PENDING_EVENTS.get().events().get(this).get();
            if (event != null) return (T) event.invoker();
            else return null;
        } else return invoker();
    }

    @Override
    public T invoker() {
        var event = this.event.get();
        if (event == null) return null;
        return event.invoker();
    }

    @Override
    public void register(Identifier id, MapCodec<? extends T> type) {
        Registry.register(types, id, type);
    }


    @Override
    public FabricDataGenerator.Pack.RegistryDependentFactory<DataProvider> provider() {
        return (output, registries) -> provider(
                callback -> callback.unwrapKey().map(it -> it.identifier().getNamespace().equals(output.getModId())).orElse(false),
                phase -> phase.unwrapKey().map(it -> it.identifier().getNamespace().equals(output.getModId())).orElse(false)
        ).create(output, registries);
    }

    @Override
    public FabricDataGenerator.Pack.RegistryDependentFactory<DataProvider> provider(Predicate<Holder<Phased<T>>> callbackPredicate, Predicate<Holder<PhaseData>> phasePredicate) {
        return (output, registries) -> new FabricDynamicRegistryProvider(output, registries) {
            @Override
            protected void configure(HolderLookup.Provider registries, Entries entries) {
                registries.lookupOrThrow(ReloadableEventImpl.this.callbacks).listElements().filter(callbackPredicate).forEach(entries::add);
                registries.lookupOrThrow(ReloadableEventImpl.this.phases).listElements().filter(phasePredicate).forEach(entries::add);
            }

            @Override
            public @NonNull String getName() {
                return "Reloadable Event " + id + " For " + output.getModId();
            }
        };
    }

    @SuppressWarnings("unchecked")
    @Override
    public MultiRegistryBootstrap bootstrap(Bootstrap<T> bootstrap, ResourceKey<? extends Registry<?>>... additionalRegistries) {
        ResourceKey<? extends Registry<?>>[] requestedRegistries = new ResourceKey[additionalRegistries.length + 2];
        System.arraycopy(additionalRegistries, 0, requestedRegistries, 0, additionalRegistries.length);
        requestedRegistries[additionalRegistries.length] = callbacks;
        requestedRegistries[additionalRegistries.length + 1] = phases;
        return new MultiRegistryBootstrap() {
            @Override
            public Set<ResourceKey<? extends Registry<?>>> requestedRegistries() {
                return Set.of(requestedRegistries);
            }

            @Override
            public void run(BootstrapGetter registries) {
                bootstrap.create(callbacks, phases).accept(registries);
            }
        };
    }

    public Event<T> createFromRegistry(RegistryAccess.Frozen frozen) {
        var event = EventFactory.createArrayBacked(eventClass, invokerFactory);
        var registries = frozen.lookup(this.callbacks).flatMap(
                callbacks -> frozen.lookup(this.phases)
                        .map(phases -> Pair.of(callbacks, phases))
        );
        registries.ifPresent(it -> {
            MutableInt callbacks = new MutableInt();
            MutableInt phases = new MutableInt();
            it.getSecond()
                    .listElements()
                    .filter(phase -> !phase.key().identifier().equals(Event.DEFAULT_PHASE))
                    .forEach(phase -> {
                        var id = phase.key().identifier();
                        phase.value().before().forEach(before -> event.addPhaseOrdering(id, before));
                        phase.value().after().forEach(after -> event.addPhaseOrdering(after, id));
                        phases.increment();
                    });

            it.getFirst().forEach(phased -> {
                event.register(phased.phase(), phased.callback());
                callbacks.increment();
            });
            LOGGER.debug("Bound {} Callbacks and {} Phases for {}", callbacks.get(), phases.get(), id);
        });

        return event;
    }

    public void bindFromRegistry(RegistryAccess.Frozen frozen) {
        this.event.accept(createFromRegistry(frozen));
    }

    public void unbind() {
        this.event.accept(null);
    }

    public static <T extends MapTyped<T>> ReloadableEvent<T> createAndRegister(Identifier id, Class<T> eventClass, Function<T[], T> invokerFactory) {
        return Registry.register(REGISTRY, id, new ReloadableEventImpl<>(id, eventClass, invokerFactory));
    }

    static {
        ReloadableEventsHooks.initialize();
    }
}
