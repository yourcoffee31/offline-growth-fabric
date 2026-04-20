package ru.floweykill27.offlinegrowth;

import net.minecraft.world.entity.AgeableMob;

public final class OfflineMobGrowth {
    private OfflineMobGrowth() {
    }

    public static void apply(AgeableMob mob, long elapsedTicks) {
        if (elapsedTicks <= 0L) {
            return;
        }

        int currentAge = mob.getAge();
        if (currentAge >= 0) {
            return;
        }

        long updatedAge = Math.min(0L, currentAge + elapsedTicks);
        mob.setAge((int) updatedAge);
    }
}
