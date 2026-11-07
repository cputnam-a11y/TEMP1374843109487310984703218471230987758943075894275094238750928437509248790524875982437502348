package io.github.cputnama11y.reloadableevent;

import io.github.cputnama11y.reloadableevent.test.Listener;
import io.github.cputnama11y.reloadableevent.test.Speaker;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.minecraft.resources.Identifier;

import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class ReloadableEvents implements ModInitializer {
    public static final String MOD_ID = "reloadableevent";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        Listener.init();
        ResourceLoader.get(PackType.SERVER_DATA).registerReloadListener(id("test"), new PreparableReloadListener() {
            @Override
            public CompletableFuture<Void> reload(SharedState currentReload, Executor taskExecutor, PreparationBarrier preparationBarrier, Executor reloadExecutor) {
                return CompletableFuture.runAsync(() -> {
                    Listener.EVENT.pendingInvoker().listenTo("Within Reload");
                }, reloadExecutor);
            }
        });

        ServerLifecycleEvents.SERVER_STARTED.register(_ -> {
            Listener.EVENT.pendingInvoker().listenTo("The Odyssey");
        });
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
}
