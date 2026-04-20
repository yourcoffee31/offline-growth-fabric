package ru.floweykill27.offlinegrowth;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.level.chunk.LevelChunk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class OfflineGrowthMod implements ModInitializer {
    public static final String MOD_ID = "offline_growth";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        ServerChunkEvents.CHUNK_UNLOAD.register(this::onChunkUnload);
        ServerChunkEvents.CHUNK_LOAD.register(this::onChunkLoad);

        ServerEntityEvents.ENTITY_UNLOAD.register((entity, world) -> {
            if (entity instanceof AgeableMob) {
                OfflineGrowthSavedData.get(world).recordEntityUnload(entity.getUUID(), world.getGameTime());
            }
        });

        ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
            if (entity instanceof AgeableMob mob) {
                long elapsedTicks = OfflineGrowthSavedData.get(world).consumeEntityElapsed(entity.getUUID(), world.getGameTime());
                OfflineMobGrowth.apply(mob, elapsedTicks);
            }
        });

        LOGGER.info("Offline Growth initialized");
    }

    private void onChunkUnload(ServerLevel world, LevelChunk chunk) {
        OfflineGrowthSavedData.get(world).recordChunkUnload(chunk.getPos().toLong(), world.getGameTime());
    }

    private void onChunkLoad(ServerLevel world, LevelChunk chunk) {
        long elapsedTicks = OfflineGrowthSavedData.get(world).consumeChunkElapsed(chunk.getPos().toLong(), world.getGameTime());
        OfflinePlantGrowth.apply(world, chunk, elapsedTicks);
    }
}
