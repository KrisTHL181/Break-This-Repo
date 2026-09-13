package net.minecraft.world.entity.ai.sensing;

import com.google.common.collect.ImmutableSet;
import java.util.Set;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;

public class HurtBySensor extends Sensor<LivingEntity> {
   @Override
   public Set<MemoryModuleType<?>> requires() {
      return ImmutableSet.of(MemoryModuleType.HURT_BY, MemoryModuleType.HURT_BY_ENTITY);
   }

   @Override
   protected void doTick(final ServerLevel level, final LivingEntity body) {
      Brain<?> brain = body.getBrain();
      DamageSource damageSource = body.getLastDamageSource();
      if (damageSource != null) {
         brain.setMemory(MemoryModuleType.HURT_BY, body.getLastDamageSource());
         if (damageSource.getEntity() instanceof LivingEntity livingEntity) {
            brain.setMemory(MemoryModuleType.HURT_BY_ENTITY, livingEntity);
         }
      } else {
         brain.eraseMemory(MemoryModuleType.HURT_BY);
      }

      brain.getMemory(MemoryModuleType.HURT_BY_ENTITY).ifPresent(hurtByEntity -> {
         if (!hurtByEntity.isAlive() || hurtByEntity.level() != level) {
            brain.eraseMemory(MemoryModuleType.HURT_BY_ENTITY);
         }
      });
   }
}
