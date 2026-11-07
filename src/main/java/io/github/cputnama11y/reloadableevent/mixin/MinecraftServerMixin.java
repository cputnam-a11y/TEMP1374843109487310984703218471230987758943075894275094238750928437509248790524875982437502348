package io.github.cputnama11y.reloadableevent.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import io.github.cputnama11y.reloadableevent.impl.ReloadableEventsHooks;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Mixin(MinecraftServer.class)
public class MinecraftServerMixin {
    @WrapMethod(
            method = "reloadResources"
    )
    private CompletableFuture<Void> wrapReload(Collection<String> packsToEnable, Operation<CompletableFuture<Void>> original) {
        return ReloadableEventsHooks.getWithPendingEvents(
                ReloadableEventsHooks.getCurrentOrCreate(),
                () -> original.call(packsToEnable)
        );

    }

    // we compose the executor for every method taking an executor in the target method. the non-async chaining methods that don't take an executor use the executor from the previous stage it seems.
    @ModifyArg(
            method = "reloadResources",
            at = {
                    @At(
                            value = "INVOKE",
                            target = "Ljava/util/concurrent/CompletableFuture;supplyAsync(Ljava/util/function/Supplier;Ljava/util/concurrent/Executor;)Ljava/util/concurrent/CompletableFuture;"
                    ),
                    @At(
                            value = "INVOKE",
                            target = "Ljava/util/concurrent/CompletableFuture;thenAcceptAsync(Ljava/util/function/Consumer;Ljava/util/concurrent/Executor;)Ljava/util/concurrent/CompletableFuture;"
                    )
            }
    )
    private Executor composeExecutor(Executor executor) {
        return ReloadableEventsHooks.withPendingEvents(ReloadableEventsHooks.PENDING_EVENTS.get(), executor);
    }
}
