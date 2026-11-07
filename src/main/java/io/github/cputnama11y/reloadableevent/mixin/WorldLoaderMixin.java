package io.github.cputnama11y.reloadableevent.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import io.github.cputnama11y.reloadableevent.impl.ReloadableEventsHooks;
import net.minecraft.server.WorldLoader;
import org.spongepowered.asm.mixin.Mixin;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Mixin(WorldLoader.class)
public class WorldLoaderMixin {
    @WrapMethod(
            method = "load"
    )
    private static <R, D> CompletableFuture<R> wrapLoad(WorldLoader.InitConfig config, WorldLoader.WorldDataSupplier<D> worldDataSupplier, WorldLoader.ResultFactory<D, R> resultFactory, Executor backgroundExecutor, Executor mainThreadExecutor, Operation<CompletableFuture<R>> original) {
        var events = ReloadableEventsHooks.getCurrentOrCreate();
        return ReloadableEventsHooks.getWithPendingEvents(
                events,
                () -> original.call(
                        config,
                        worldDataSupplier,
                        resultFactory,
                        ReloadableEventsHooks.withPendingEvents(events, backgroundExecutor),
                        ReloadableEventsHooks.withPendingEvents(events, mainThreadExecutor)
                )
        );
    }
}
