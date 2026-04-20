package ru.floweykill27.offlinegrowth;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class OfflineGrowthSavedData extends SavedData {
    private static final String SAVE_ID = OfflineGrowthMod.MOD_ID + "_offline_growth";
    private static final String CHUNKS_TAG = "chunks";
    private static final String ENTITIES_TAG = "entities";

    private static final Codec<Map<Long, Long>> LONG_LONG_MAP_CODEC = Codec.unboundedMap(
            Codec.STRING.xmap(Long::parseLong, String::valueOf),
            Codec.LONG
    );

    private static final Codec<Map<UUID, Long>> UUID_LONG_MAP_CODEC = Codec.unboundedMap(
            Codec.STRING.xmap(UUID::fromString, UUID::toString),
            Codec.LONG
    );

    private static final Codec<OfflineGrowthSavedData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            LONG_LONG_MAP_CODEC.optionalFieldOf(CHUNKS_TAG, Map.of()).forGetter(data -> data.chunkUnloadTimes),
            UUID_LONG_MAP_CODEC.optionalFieldOf(ENTITIES_TAG, Map.of()).forGetter(data -> data.entityUnloadTimes)
    ).apply(instance, OfflineGrowthSavedData::new));

    public static final SavedDataType<OfflineGrowthSavedData> TYPE =
            new SavedDataType<>(SAVE_ID, OfflineGrowthSavedData::new, CODEC, null);

    private final Map<Long, Long> chunkUnloadTimes = new HashMap<>();
    private final Map<UUID, Long> entityUnloadTimes = new HashMap<>();

    public OfflineGrowthSavedData() {
    }

    private OfflineGrowthSavedData(Map<Long, Long> chunkUnloadTimes, Map<UUID, Long> entityUnloadTimes) {
        this.chunkUnloadTimes.putAll(chunkUnloadTimes);
        this.entityUnloadTimes.putAll(entityUnloadTimes);
    }

    public static OfflineGrowthSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    public void recordChunkUnload(long chunkPos, long gameTime) {
        chunkUnloadTimes.put(chunkPos, gameTime);
        setDirty();
    }

    public long consumeChunkElapsed(long chunkPos, long currentGameTime) {
        Long storedTime = chunkUnloadTimes.remove(chunkPos);
        if (storedTime == null || currentGameTime <= storedTime) {
            return 0L;
        }
        setDirty();
        return currentGameTime - storedTime;
    }

    public void recordEntityUnload(UUID entityId, long gameTime) {
        entityUnloadTimes.put(entityId, gameTime);
        setDirty();
    }

    public long consumeEntityElapsed(UUID entityId, long currentGameTime) {
        Long storedTime = entityUnloadTimes.remove(entityId);
        if (storedTime == null || currentGameTime <= storedTime) {
            return 0L;
        }
        setDirty();
        return currentGameTime - storedTime;
    }
}
