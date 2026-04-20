package ru.floweykill27.offlinegrowth;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.BambooStalkBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CactusBlock;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.GrowingPlantHeadBlock;
import net.minecraft.world.level.block.KelpBlock;
import net.minecraft.world.level.block.MangrovePropaguleBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.PitcherCropBlock;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.StemBlock;
import net.minecraft.world.level.block.SugarCaneBlock;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;

public final class OfflinePlantGrowth {
    private static final int MAX_RANDOM_TICK_ATTEMPTS_PER_BLOCK = 256;

    private OfflinePlantGrowth() {
    }

    public static void apply(ServerLevel world, LevelChunk chunk, long elapsedTicks) {
        if (elapsedTicks <= 0L) {
            return;
        }

        int randomTickSpeed = world.getGameRules().getInt(GameRules.RULE_RANDOMTICKING);
        if (randomTickSpeed <= 0) {
            return;
        }

        int attempts = Mth.clamp((int) ((elapsedTicks * (long) randomTickSpeed + 4095L) / 4096L), 1, MAX_RANDOM_TICK_ATTEMPTS_PER_BLOCK);
        RandomSource random = RandomSource.create(chunk.getPos().toLong() ^ world.getGameTime());
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        LevelChunkSection[] sections = chunk.getSections();
        int minBuildHeight = world.getMinBuildHeight();

        for (int sectionIndex = 0; sectionIndex < sections.length; sectionIndex++) {
            LevelChunkSection section = sections[sectionIndex];
            if (section == null || section.hasOnlyAir()) {
                continue;
            }

            int baseY = minBuildHeight + (sectionIndex << 4);
            for (int localY = 0; localY < 16; localY++) {
                for (int localZ = 0; localZ < 16; localZ++) {
                    for (int localX = 0; localX < 16; localX++) {
                        BlockState initialState = section.getBlockState(localX, localY, localZ);
                        if (!supportsOfflineGrowth(initialState)) {
                            continue;
                        }

                        pos.set(chunk.getPos().getMinBlockX() + localX, baseY + localY, chunk.getPos().getMinBlockZ() + localZ);
                        simulateRandomTicks(world, pos, attempts, random);
                    }
                }
            }
        }
    }

    private static void simulateRandomTicks(ServerLevel world, BlockPos pos, int attempts, RandomSource random) {
        for (int i = 0; i < attempts; i++) {
            BlockState state = world.getBlockState(pos);
            if (!supportsOfflineGrowth(state) || !state.isRandomlyTicking()) {
                return;
            }

            state.randomTick(world, pos, random);
        }
    }

    private static boolean supportsOfflineGrowth(BlockState state) {
        Block block = state.getBlock();
        return block instanceof CropBlock
                || block instanceof StemBlock
                || block instanceof SaplingBlock
                || block instanceof MangrovePropaguleBlock
                || block instanceof BambooStalkBlock
                || block instanceof CactusBlock
                || block instanceof SugarCaneBlock
                || block instanceof SweetBerryBushBlock
                || block instanceof CocoaBlock
                || block instanceof NetherWartBlock
                || block instanceof PitcherCropBlock
                || block instanceof GrowingPlantHeadBlock
                || block instanceof KelpBlock
                || block == Blocks.TORCHFLOWER_CROP
                || block == Blocks.CHORUS_FLOWER;
    }
}
