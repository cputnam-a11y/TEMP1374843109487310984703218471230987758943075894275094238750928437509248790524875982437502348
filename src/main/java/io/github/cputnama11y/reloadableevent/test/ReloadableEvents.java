package io.github.cputnama11y.reloadableevent.test;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.util.Unit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReloadableEvents implements ModInitializer {
    public static final String MOD_ID = "reloadableevent";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        Listener.init();
        ResourceLoader.get(PackType.SERVER_DATA).registerReloadListener(id("test"), (_, _, preparationBarrier, reloadExecutor) -> preparationBarrier.wait(Unit.INSTANCE).thenRunAsync(
                () -> Listener.EVENT.pendingInvoker().listenTo("Within Reload"),
                reloadExecutor
        ));

        ServerLifecycleEvents.SERVER_STARTED.register(_ -> Listener.EVENT.pendingInvoker().listenTo("The Odyssey"));
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
}
