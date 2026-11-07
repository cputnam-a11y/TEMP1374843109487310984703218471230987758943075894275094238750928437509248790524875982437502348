package io.github.cputnama11y.reloadableevent.test;

import io.github.cputnama11y.reloadableevent.api.ReloadableEvent;
import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.resources.ResourceKey;

import static io.github.cputnama11y.reloadableevent.test.ReloadableEvents.id;

public class ReloadableEventDatagen implements DataGeneratorEntrypoint {
    @Override
    public void onInitializeDataGenerator(FabricDataGenerator fabricDataGenerator) {
        fabricDataGenerator.createPack().addProvider(Listener.EVENT.provider());
    }

    @Override
    public void buildReloadableRegistry(RegistrySetBuilder registryBuilder) {
        registryBuilder.add(Listener.EVENT.bootstrap((callbacks, phases) -> registries -> registries.get(callbacks).register(ResourceKey.create(callbacks, id("test")), new ReloadableEvent.Phased<>(new Speaker()))));
    }
}
