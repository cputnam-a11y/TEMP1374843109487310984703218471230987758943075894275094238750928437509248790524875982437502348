package io.github.cputnama11y.reloadableevent.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import io.github.cputnama11y.reloadableevent.impl.ReloadableEventsHooks;
import net.minecraft.core.LayeredRegistryAccess;
import net.minecraft.core.RegistryAccess;
import net.minecraft.server.RegistryLayer;
import net.minecraft.server.ReloadableServerRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Mixin(ReloadableServerRegistries.class)
public class ReloadableServerRegistriesMixin {
    @ModifyExpressionValue(
            method = "reload",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/resources/RegistryDataLoader;load(Lnet/minecraft/server/packs/resources/ResourceManager;Ljava/util/List;Ljava/util/List;Ljava/util/concurrent/Executor;)Ljava/util/concurrent/CompletableFuture;")
    )
    private static CompletableFuture<RegistryAccess.Frozen> bindEvents(CompletableFuture<RegistryAccess.Frozen> original, final LayeredRegistryAccess<RegistryLayer> context, @Local(argsOnly = true, name = "executor") Executor executor) {
        return original.thenApplyAsync(frozen -> {
            if (ReloadableEventsHooks.PENDING_EVENTS.isBound()) {
                ReloadableEventsHooks.bindForReload(context.replaceFrom(RegistryLayer.RELOADABLE, frozen).compositeAccess(), ReloadableEventsHooks.PENDING_EVENTS.get());
            }
            // do events
            return frozen;
        }, executor);
    }
}
