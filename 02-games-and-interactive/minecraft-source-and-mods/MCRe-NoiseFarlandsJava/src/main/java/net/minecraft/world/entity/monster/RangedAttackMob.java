package net.minecraft.world.entity.monster;

import net.minecraft.world.entity.LivingEntity;

public interface RangedAttackMob {
    void performRangedAttack(LivingEntity target, float power);
}