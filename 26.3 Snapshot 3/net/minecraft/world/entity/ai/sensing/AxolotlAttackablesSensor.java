package net.minecraft.world.entity.ai.sensing;

import com.google.common.collect.Sets;
import java.util.Set;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;

public class AxolotlAttackablesSensor extends NearestVisibleLivingEntitySensor {
   public static final float TARGET_DETECTION_DISTANCE = 8.0F;

   @Override
   protected boolean isMatchingEntity(final ServerLevel level, final LivingEntity body, final LivingEntity mob) {
      return this.isClose(body, mob)
         && mob.isInWater()
         && (this.isHostileTarget(mob) || this.isHuntTarget(body, mob))
         && Sensor.isEntityAttackable(level, body, mob);
   }

   private boolean isHuntTarget(final LivingEntity body, final LivingEntity mob) {
      return !body.getBrain().hasMemoryValue(MemoryModuleType.HAS_HUNTING_COOLDOWN) && mob.is(EntityTypeTags.AXOLOTL_HUNT_TARGETS);
   }

   private boolean isHostileTarget(final LivingEntity mob) {
      return mob.is(EntityTypeTags.AXOLOTL_ALWAYS_HOSTILES);
   }

   private boolean isClose(final LivingEntity body, final LivingEntity mob) {
      return mob.distanceToSqr(body) <= 64.0;
   }

   @Override
   protected MemoryModuleType<LivingEntity> getMemoryToSet() {
      return MemoryModuleType.NEAREST_ATTACKABLE;
   }

   @Override
   public Set<MemoryModuleType<?>> requires() {
      return Sets.union(super.requires(), Set.of(MemoryModuleType.HAS_HUNTING_COOLDOWN));
   }
}
