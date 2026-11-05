package io.github.cputnama11y.reloadableevent.mixin;

import io.github.cputnama11y.reloadableevent.impl.Duck;
import io.github.cputnama11y.reloadableevent.impl.util.mutableobject.MutableConstantObject;
import net.minecraft.commands.Commands;
import net.minecraft.core.RegistryAccess;
import net.minecraft.server.ReloadableServerRegistries;
import net.minecraft.server.ReloadableServerResources;
import net.minecraft.server.permissions.PermissionSet;
import net.minecraft.world.flag.FeatureFlagSet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(ReloadableServerResources.class)
public class ReloadableServerResourcesMixin implements Duck {
    private final MutableConstantObject<RegistryAccess.Frozen> layers = MutableConstantObject.create();
    @Inject(
            method = "<init>",
            at = @At("CTOR_HEAD")
    )
    private void a(ReloadableServerRegistries.LoadResult loadingContext, FeatureFlagSet enabledFeatures, Commands.CommandSelection commandSelection, List<?> postponedTags, PermissionSet functionCompilationPermissions, List<?> newComponents, CallbackInfo ci) {
        layers.accept(loadingContext.layers().compositeAccess());
    }


    @Override
    public RegistryAccess.Frozen reloadableevent$getAccess() {
        return layers.get();
    }
}
