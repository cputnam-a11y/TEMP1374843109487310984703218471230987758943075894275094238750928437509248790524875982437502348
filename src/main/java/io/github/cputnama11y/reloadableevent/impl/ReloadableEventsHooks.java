package io.github.cputnama11y.reloadableevent.impl;

import io.github.cputnama11y.reloadableevent.api.ReloadableEvent;
import io.github.cputnama11y.reloadableevent.impl.util.mutableobject.MutableConstantObject;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

public class ReloadableEventsHooks {
    public static final ScopedValue<PendingEvents> PENDING_EVENTS = ScopedValue.newInstance();
    public static final Identifier KILL_PHASE = Identifier.fromNamespaceAndPath("fabric", "kill_old_events");
    public static final Identifier UPDATE_PHASE = Identifier.fromNamespaceAndPath("fabric", "update_events");

    public static void initialize() {}

    public static <T> ResourceKey<Registry<T>> makeReloadableEventKey(Identifier eventId, String subRegistry) {
        return ResourceKey.createRegistryKey(
                Identifier.fromNamespaceAndPath("fabric", subRegistry)
                        .withSuffix("/" + eventId.getNamespace())
                        .withSuffix("/" + eventId.getPath())
        );
    }

    private static void bindALl(MinecraftServer server) {
        ReloadableEventImpl.REGISTRY.forEach(
                event -> event.bindFromRegistry(
                        ((RegistryAccessAccess) server.resources.managers()).reloadableevent$getAccess()
                )
        );
    }

    public static void bindForReload(RegistryAccess.Frozen frozen, PendingEvents events) {
        ReloadableEventImpl.REGISTRY.forEach(event -> {
            var eventImpl = (ReloadableEventImpl<?>) event;
            events.events().get(eventImpl).accept(eventImpl.createFromRegistry(frozen));
        });
    }

    public record PendingEvents(Map<ReloadableEvent<?>, MutableConstantObject<Event<?>>> events) {
    }

    public static PendingEvents buildPendingEvents() {
        HashMap<ReloadableEvent<?>, MutableConstantObject<Event<?>>> map = new HashMap<>();

        for (var event : ReloadableEventImpl.REGISTRY) {
            map.put(event, MutableConstantObject.create());
        }

        return new PendingEvents(Map.copyOf(map));
    }

    public static Executor withPendingEvents(PendingEvents events, Executor executor) {
        return task -> executor.execute(() -> ScopedValue.where(PENDING_EVENTS, events).run(task));
    }

    public static <T> T getWithPendingEvents(PendingEvents events, Supplier<T> action) {
        return ScopedValue.where(
                ReloadableEventsHooks.PENDING_EVENTS,
                events
        ).call(
                action::get
        );
    }

    public static PendingEvents getCurrentOrCreate() {
        if (PENDING_EVENTS.isBound()) return PENDING_EVENTS.get();
        return buildPendingEvents();
    }

    static {
        ServerLifecycleEvents.END_DATA_PACK_RELOAD.addPhaseOrdering(UPDATE_PHASE, Event.DEFAULT_PHASE);
        ServerLifecycleEvents.SERVER_STARTING.addPhaseOrdering(UPDATE_PHASE, Event.DEFAULT_PHASE);
        ServerLifecycleEvents.SERVER_STOPPED.addPhaseOrdering(Event.DEFAULT_PHASE, KILL_PHASE);
        ServerLifecycleEvents.SERVER_STARTING.register(UPDATE_PHASE, ReloadableEventsHooks::bindALl);
        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register(UPDATE_PHASE, (server, _, success) -> {
            if (success) {
                bindALl(server);
            }
        });

        ServerLifecycleEvents.SERVER_STOPPED.register(KILL_PHASE, _ -> ReloadableEventImpl.REGISTRY.forEach(ReloadableEventImpl::unbind));
    }
}
