package io.github.cputnama11y.reloadableevent.impl;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.cputnama11y.reloadableevent.api.ReloadableEvent;
import io.github.cputnama11y.reloadableevent.impl.util.mutableobject.MutableConstantObject;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.registry.DynamicRegistries;
import net.fabricmc.fabric.api.event.registry.FabricRegistryBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Unit;
import org.apache.commons.lang3.mutable.MutableObject;

import java.util.List;
import java.util.function.Function;

public record ReloadableEventImpl<T extends ReloadableEvent.MapTyped<T>>(Identifier key, MutableConstantObject<Event<T>> event, Registry<MapCodec<? extends T>> types, ResourceKey<? extends Registry<PhaseData>> phases, ResourceKey<? extends Registry<Phased<T>>> callbacks, boolean applicableDuringReload, MutableObject<Event<T>> backup, Class<T> eventClass, Function<T[], T> invokerFactory) implements ReloadableEvent<T> {
    public static final MapCodec<? extends MapTyped<?>> INVOKER_TYPE = MapCodec.unit(Unit.INSTANCE).flatXmap(
            _ -> DataResult.error(() -> "Cannot deserialize invoker"),
            _ -> DataResult.error(() -> "Cannot serialize invoker")
    );
    public static final ResourceKey<Registry<ReloadableEvent<?>>> KEY = ResourceKey.createRegistryKey(
            Identifier.fromNamespaceAndPath("fabric", "reloadable_events")
    );
    public static final Registry<ReloadableEvent<?>> REGISTRY = FabricRegistryBuilder.create(KEY).buildAndRegister();

    public ReloadableEventImpl(Identifier key, boolean applicableDuringReload, Class<T> eventClass, Function<T[], T> invokerFactory) {
        this(
                key,
                MutableConstantObject.create(),
                FabricRegistryBuilder.<MapCodec<? extends T>>create(
                        ResourceKey.createRegistryKey(
                                Identifier.fromNamespaceAndPath("fabric", "callback_type")
                                        .withSuffix("/" + key.getNamespace())
                                        .withSuffix("/" + key.getPath())
                        )
                ).buildAndRegister(),
                ResourceKey.createRegistryKey(
                        Identifier.fromNamespaceAndPath("fabric", "event_phases")
                                .withSuffix("/" + key.getNamespace())
                                .withSuffix("/" + key.getPath())
                ),
                ResourceKey.createRegistryKey(
                        Identifier.fromNamespaceAndPath("fabric", "reloadable_events")
                                .withSuffix("/" + key.getNamespace())
                                .withSuffix("/" + key.getPath())
                ),
                applicableDuringReload,
                new MutableObject<>(),
                eventClass, invokerFactory
        );
        //noinspection unchecked
        Registry.register(this.types, Identifier.fromNamespaceAndPath("fabric", "invoker"), (MapCodec<? extends T>) INVOKER_TYPE);

        DynamicRegistries.registerReloadable(this.phases, PhaseData.CODEC);
        DynamicRegistries.registerReloadable(this.callbacks, Phased.<T>codec(this.types.byNameCodec().dispatchMap(
                MapTyped::type,
                Function.identity()
        )).codec());
    }

    private void restoreBackupIfPresent() {
        var backup = this.backup.get();
        if (backup != null) event.accept(backup);
    }

    private void unbind() {
        this.event.accept(null);
        this.backup.setValue(null);
    }

    private void discardBackup() {
        this.backup.setValue(null);
    }

    public void fillFromRegistry(RegistryAccess.Frozen frozen, boolean isReload) {
        if (this.applicableDuringReload() == isReload) {
            if (this.applicableDuringReload()) {
                this.backup.setValue(this.event.get());
            }

            var event = EventFactory.createArrayBacked(eventClass, invokerFactory);
            var registries = frozen.lookup(this.callbacks).flatMap(callbacks -> frozen.lookup(this.phases).map(phases -> Pair.of(callbacks, phases)));
            registries.ifPresent(it -> {
                it.getSecond().listElements().forEach(phase -> {
                    var id = phase.key().identifier();
                    if (id.equals(Event.DEFAULT_PHASE)) return;
                    phase.value().before.forEach(before -> event.addPhaseOrdering(id, before));
                    phase.value().after.forEach(after -> event.addPhaseOrdering(after, id));
                });

                it.getFirst().forEach(phased -> event.register(phased.phase(), phased.callback()));
            });

            this.event.accept(event);
        }
    }

    @Override
    public T invoker() {
        var event = this.event.get();
        if (event == null) return null;
        return event.invoker();
    }

    @Override
    public void registerType(Identifier id, MapCodec<? extends T> type) {
        Registry.register(types, id, type);
    }

    record PhaseData(List<Identifier> before, List<Identifier> after) {
        public static final Codec<PhaseData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Identifier.CODEC.listOf().fieldOf("before").forGetter(PhaseData::before),
                Identifier.CODEC.listOf().fieldOf("after").forGetter(PhaseData::after)
        ).apply(instance, PhaseData::new));
    }

    record Phased<T>(T callback, Identifier phase) {
        public static <T> MapCodec<Phased<T>> codec(MapCodec<T> codec) {
            return RecordCodecBuilder.mapCodec(instance -> instance.group(
                    codec.forGetter(Phased::callback),
                    Identifier.CODEC.optionalFieldOf("phase", Event.DEFAULT_PHASE).forGetter(Phased::phase)
            ).apply(instance, Phased::new));
        }
    }

    static {
        Identifier repairPhase = Identifier.fromNamespaceAndPath("fabric", "repair_failed_events");
        Identifier bindPhase = Identifier.fromNamespaceAndPath("fabric", "bind_new_events");
        Identifier killPhase = Identifier.fromNamespaceAndPath("fabric", "kill_old_events");
        ServerLifecycleEvents.END_DATA_PACK_RELOAD.addPhaseOrdering(repairPhase, Event.DEFAULT_PHASE);
        ServerLifecycleEvents.END_DATA_PACK_RELOAD.addPhaseOrdering(bindPhase, Event.DEFAULT_PHASE);
        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register(repairPhase, (server, resourceManager, success) -> {
            if (!success) {
                REGISTRY.forEach(event -> {
                    var eventImpl = (ReloadableEventImpl<?>) event;
                    if (eventImpl.applicableDuringReload()) {
                        eventImpl.restoreBackupIfPresent();
                    }
                });
            }
        });
        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register(bindPhase, (server, resourceManager, success) -> {
            if (success) {
                REGISTRY.forEach(event -> {
                    var eventImpl = (ReloadableEventImpl<?>) event;
                    if (eventImpl.applicableDuringReload()) {
                        eventImpl.discardBackup();
                    } else {
                        eventImpl.fillFromRegistry(((Duck) server.resources.managers()).reloadableevent$getAccess(), false);
                    }
                });
            }
        });

        ServerLifecycleEvents.SERVER_STOPPED.register(killPhase, server -> {

        });
    }

    public static void bindEarlyReloadable(RegistryAccess.Frozen frozen) {
        REGISTRY.forEach(event -> {
            var eventImpl = (ReloadableEventImpl<?>) event;
            if (eventImpl.applicableDuringReload()) {
                eventImpl.fillFromRegistry(frozen, true);
            }
        });
    }

    public static <T extends MapTyped<T>> ReloadableEvent<T> createAndRegister(Identifier id, Class<T> eventClass, Function<T[], T> invokerFactory) {
        return Registry.register(REGISTRY, id, new ReloadableEventImpl<T>(id, false, eventClass, invokerFactory));
    }

    public static <T extends MapTyped<T>> ReloadableEvent<T> createEarlyReloadAndRegister(Identifier id, Class<T> eventClass, Function<T[], T> invokerFactory) {
        return Registry.register(REGISTRY, id, new ReloadableEventImpl<T>(id, true, eventClass, invokerFactory));
    }

}
