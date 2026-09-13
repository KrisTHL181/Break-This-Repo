package net.minecraft.world.entity.ai.sensing;

import com.google.common.collect.ImmutableSet;
import java.util.Set;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;

public class HurtBySensor extends Sensor<LivingEntity> {
   @Override
   public Set<MemoryModuleType<?>> requires() {
      return ImmutableSet.of(MemoryModuleType.HURT_BY, MemoryModuleType.HURT_BY_ENTITY);
   }

   @Override
   protected void doTick(ServerLevel p_26670_, LivingEntity p_26671_) {
      Brain<?> brain = p_26671_.getBrain();
      DamageSource damagesource = p_26671_.getLastDamageSource();
      if (damagesource != null) {
         brain.setMemory(MemoryModuleType.HURT_BY, p_26671_.getLastDamageSource());
         Entity entity = damagesource.getEntity();
         if (entity instanceof LivingEntity) {
            brain.setMemory(MemoryModuleType.HURT_BY_ENTITY, (LivingEntity)entity);
         }
      } else {
         brain.eraseMemory(MemoryModuleType.HURT_BY);
      }

      brain.getMemory(MemoryModuleType.HURT_BY_ENTITY).ifPresent(p_449607_ -> {
         if (!p_449607_.isAlive() || p_449607_.level() != p_26670_) {
            brain.eraseMemory(MemoryModuleType.HURT_BY_ENTITY);
         }
      });
   }
}
