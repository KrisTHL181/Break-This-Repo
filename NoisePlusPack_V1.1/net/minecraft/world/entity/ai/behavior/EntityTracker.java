package net.minecraft.world.entity.ai.behavior;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.NearestVisibleLivingEntities;
import net.minecraft.world.phys.Vec3;

public class EntityTracker implements PositionTracker {
   private final Entity entity;
   private final boolean trackEyeHeight;
   private final boolean targetEyeHeight;

   public EntityTracker(Entity p_22849_, boolean p_22850_) {
      this(p_22849_, p_22850_, false);
   }

   public EntityTracker(Entity p_410542_, boolean p_408612_, boolean p_409986_) {
      this.entity = p_410542_;
      this.trackEyeHeight = p_408612_;
      this.targetEyeHeight = p_409986_;
   }

   @Override
   public Vec3 currentPosition() {
      return this.trackEyeHeight ? this.entity.position().add(0.0, this.entity.getEyeHeight(), 0.0) : this.entity.position();
   }

   @Override
   public BlockPos currentBlockPosition() {
      return this.targetEyeHeight ? BlockPos.containing(this.entity.getEyePosition()) : this.entity.blockPosition();
   }

   @Override
   public boolean isVisibleBy(LivingEntity p_22853_) {
      if (this.entity instanceof LivingEntity livingentity) {
         if (!livingentity.isAlive()) {
            return false;
         }

         Optional<NearestVisibleLivingEntities> optional = p_22853_.getBrain().getMemory(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES);
         return optional.isPresent() && optional.get().contains(livingentity);
      } else {
         return true;
      }
   }

   public Entity getEntity() {
      return this.entity;
   }

   @Override
   public String toString() {
      return "EntityTracker for " + this.entity;
   }
}
