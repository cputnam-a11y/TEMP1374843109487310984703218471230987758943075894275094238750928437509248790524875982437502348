package io.github.cputnama11y.reloadableevent.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import io.github.cputnama11y.reloadableevent.api.ReloadableEvent;
import io.github.cputnama11y.reloadableevent.impl.Duck;
import io.github.cputnama11y.reloadableevent.impl.PendingEvents;
import io.github.cputnama11y.reloadableevent.impl.ReloadableEventImpl;
import io.github.cputnama11y.reloadableevent.impl.util.mutableobject.MutableConstantObject;
import net.fabricmc.fabric.api.event.Event;
import net.minecraft.commands.Commands;
import net.minecraft.core.LayeredRegistryAccess;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.server.RegistryLayer;
import net.minecraft.server.ReloadableServerRegistries;
import net.minecraft.server.ReloadableServerResources;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.permissions.PermissionSet;
import net.minecraft.world.flag.FeatureFlagSet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Mixin(ReloadableServerResources.class)
public class ReloadableServerResourcesMixin implements Duck {
    @Unique
    private final MutableConstantObject<RegistryAccess.Frozen> layers = MutableConstantObject.create();

    @Inject(
            method = "<init>",
            at = @At("CTOR_HEAD")
    )
    private void a(ReloadableServerRegistries.LoadResult loadingContext, FeatureFlagSet enabledFeatures, Commands.CommandSelection commandSelection, List<?> postponedTags, PermissionSet functionCompilationPermissions, List<?> newComponents, CallbackInfo ci) {
        layers.accept(loadingContext.layers().compositeAccess());
    }

    @SuppressWarnings("unchecked")
    @WrapMethod(
            method = "loadResources"
    )
    private static CompletableFuture<ReloadableServerResources> wrapLoadResources(ResourceManager resourceManager, LayeredRegistryAccess<RegistryLayer> contextLayers, List<Registry.PendingTags<?>> updatedContextTags, FeatureFlagSet enabledFeatures, Commands.CommandSelection commandSelection, PermissionSet functionCompilationPermissions, Executor backgroundExecutor, Executor mainThreadExecutor, Operation<CompletableFuture<ReloadableServerResources>> original) {
        var events = new PendingEvents(buildStableMap());
        Executor newMainThreadExecutor = r -> {
            mainThreadExecutor.execute(() -> ScopedValue.where(PendingEvents.EVENTS, events).run(r));
        };
        Executor newBackgroundExecutor = r -> {
            backgroundExecutor.execute(() -> ScopedValue.where(PendingEvents.EVENTS, events).run(r));
        };
        return ScopedValue.where(PendingEvents.EVENTS, events).call(() -> original.call(resourceManager, contextLayers, updatedContextTags, enabledFeatures, commandSelection, functionCompilationPermissions, newBackgroundExecutor, newMainThreadExecutor));
    }

    @Unique
    private static Map<ReloadableEvent<?>, MutableConstantObject<Event<?>>> buildStableMap() {
        HashMap<ReloadableEvent<?>, MutableConstantObject<Event<?>>> map = new HashMap<>();
        for (var event : ReloadableEventImpl.REGISTRY) {
            map.put(event, MutableConstantObject.create());
        }
        return Map.copyOf(map);
    }


    @Override
    public RegistryAccess.Frozen reloadableevent$getAccess() {
        return layers.get();
    }
}
