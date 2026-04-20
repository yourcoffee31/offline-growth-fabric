package ru.floweykill27.offlinegrowth;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class OfflineGrowthSavedData extends SavedData {
    private static final String SAVE_ID = OfflineGrowthMod.MOD_ID;
    private static final String CHUNKS_TAG = "chunks";
    private static final String ENTITIES_TAG = "entities";
    private static final String ENTITY_ID_TAG = "id";
    private static final String TIME_TAG = "time";

    private final Map<Long, Long> chunkUnloadTimes = new HashMap<>();
    private final Map<UUID, Long> entityUnloadTimes = new HashMap<>();

    public static OfflineGrowthSavedData get(ServerLevel level) {
        DimensionDataStorage storage = level.getDataStorage();
        return storage.computeIfAbsent(new SavedData.Factory<>(OfflineGrowthSavedData::new, OfflineGrowthSavedData::load, null), SAVE_ID);
    }

// OfflineGrowthSavedData.java
private static OfflineGrowthSavedData load(CompoundTag tag) {
    OfflineGrowthSavedData data = new OfflineGrowthSavedData();

    CompoundTag chunksTag = tag.getCompoundOrEmpty(CHUNKS_TAG);
    for (String key : chunksTag.keySet()) {
        chunksTag.getLong(key).ifPresent(time ->
            data.chunkUnloadTimes.put(Long.parseLong(key), time)
        );
    }

    ListTag entities = tag.getListOrEmpty(ENTITIES_TAG);
    for (Tag raw : entities) {
        if (!(raw instanceof CompoundTag entry)) continue;

        var entityIdOpt = entry.getString(ENTITY_ID_TAG);
        var timeOpt = entry.getLong(TIME_TAG);

        if (entityIdOpt.isPresent() && timeOpt.isPresent()) {
            UUID entityId = UUID.fromString(entityIdOpt.get());
            data.entityUnloadTimes.put(entityId, timeOpt.get());
        }
    }

    return data;
}

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider lookupProvider) {
        CompoundTag chunksTag = new CompoundTag();
        for (Map.Entry<Long, Long> entry : chunkUnloadTimes.entrySet()) {
            chunksTag.putLong(Long.toString(entry.getKey()), entry.getValue());
        }
        tag.put(CHUNKS_TAG, chunksTag);

        ListTag entities = new ListTag();
        for (Map.Entry<UUID, Long> entry : entityUnloadTimes.entrySet()) {
            CompoundTag entityTag = new CompoundTag();
            entityTag.putString(ENTITY_ID_TAG, entry.getKey().toString());
            entityTag.putLong(TIME_TAG, entry.getValue());
            entities.add(entityTag);
        }
        tag.put(ENTITIES_TAG, entities);

        return tag;
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
