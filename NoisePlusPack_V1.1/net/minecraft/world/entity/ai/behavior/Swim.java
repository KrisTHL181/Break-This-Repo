package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Mob;

public class Swim<T extends Mob> extends Behavior<T> {
   private final float chance;

   public Swim(float p_24383_) {
      super(ImmutableMap.of());
      this.chance = p_24383_;
   }

   public static <T extends Mob> boolean shouldSwim(T p_327994_) {
      return p_327994_.isInWater() && p_327994_.getFluidHeight(FluidTags.WATER) > p_327994_.getFluidJumpThreshold() || p_327994_.isInLava();
   }

   protected boolean checkExtraStartConditions(ServerLevel p_24388_, Mob p_24389_) {
      return shouldSwim(p_24389_);
   }

   protected boolean canStillUse(ServerLevel p_24391_, Mob p_24392_, long p_24393_) {
      return this.checkExtraStartConditions(p_24391_, p_24392_);
   }

   protected void tick(ServerLevel p_24399_, Mob p_24400_, long p_24401_) {
      if (p_24400_.getRandom().nextFloat() < this.chance) {
         p_24400_.getJumpControl().jump();
      }
   }
}
