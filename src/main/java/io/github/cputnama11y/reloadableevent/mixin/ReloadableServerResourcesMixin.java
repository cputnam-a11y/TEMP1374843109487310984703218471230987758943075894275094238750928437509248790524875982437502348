package io.github.cputnama11y.reloadableevent.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import io.github.cputnama11y.reloadableevent.impl.RegistryAccessAccess;
import io.github.cputnama11y.reloadableevent.impl.ReloadableEventsHooks;
import io.github.cputnama11y.reloadableevent.impl.util.mutableobject.MutableConstantObject;
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

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Mixin(ReloadableServerResources.class)
public class ReloadableServerResourcesMixin implements RegistryAccessAccess {
    @Unique
    private final MutableConstantObject<RegistryAccess.Frozen> layers = MutableConstantObject.create();

    @Inject(
            method = "<init>",
            at = @At("CTOR_HEAD")
    )
    private void a(ReloadableServerRegistries.LoadResult loadingContext, FeatureFlagSet enabledFeatures, Commands.CommandSelection commandSelection, List<?> postponedTags, PermissionSet functionCompilationPermissions, List<?> newComponents, CallbackInfo ci) {
        layers.accept(loadingContext.layers().compositeAccess());
    }

    @WrapMethod(
            method = "loadResources"
    )
    private static CompletableFuture<ReloadableServerResources> wrapLoadResources(ResourceManager resourceManager, LayeredRegistryAccess<RegistryLayer> contextLayers, List<Registry.PendingTags<?>> updatedContextTags, FeatureFlagSet enabledFeatures, Commands.CommandSelection commandSelection, PermissionSet functionCompilationPermissions, Executor backgroundExecutor, Executor mainThreadExecutor, Operation<CompletableFuture<ReloadableServerResources>> original) {
        var events = ReloadableEventsHooks.getCurrentOrCreate();
        return ReloadableEventsHooks.getWithPendingEvents(
                events,
                () -> original.call(
                        resourceManager,
                        contextLayers,
                        updatedContextTags,
                        enabledFeatures,
                        commandSelection,
                        functionCompilationPermissions,
                        ReloadableEventsHooks.withPendingEvents(events, backgroundExecutor),
                        ReloadableEventsHooks.withPendingEvents(events, mainThreadExecutor)
                )
        );
    }

    @Override
    public RegistryAccess.Frozen reloadableevent$getAccess() {
        return layers.get();
    }
}
