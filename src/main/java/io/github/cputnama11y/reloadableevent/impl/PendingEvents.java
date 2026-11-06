package io.github.cputnama11y.reloadableevent.impl;

import io.github.cputnama11y.reloadableevent.api.ReloadableEvent;
import io.github.cputnama11y.reloadableevent.impl.util.mutableobject.MutableConstantObject;
import net.fabricmc.fabric.api.event.Event;

import java.util.Map;

public record PendingEvents(Map<ReloadableEvent<?>, MutableConstantObject<Event<?>>> events) {
    public static final ScopedValue<PendingEvents> EVENTS = ScopedValue.newInstance();
}
