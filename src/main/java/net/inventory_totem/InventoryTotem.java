package net.inventory_totem;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.LoggerFactory;

public final class InventoryTotem implements ModInitializer {
    public static final String MOD_ID = "inventory_totem";

    @Override
    public void onInitialize() {
        ServerLivingEntityEvents.ALLOW_DEATH.register((entity, source, amount) ->
                InventoryProtection.allowDeath(entity));
        String version = FabricLoader.getInstance().getModContainer(MOD_ID)
                .orElseThrow().getMetadata().getVersion().getFriendlyString();
        LoggerFactory.getLogger(MOD_ID).info("[Inventory Totem] Mod initialized. Version: {}", version);
    }
}
