package net.minecraft.world.entity.monster;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

public interface RangedAttackMob {
   void performRangedAttack(LivingEntity target, float power);

   default float rangedAttackUncertainty(final Level level) {
      return 14 - level.getDifficulty().getId() * 4;
   }
}
