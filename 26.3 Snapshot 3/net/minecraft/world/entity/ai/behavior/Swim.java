package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Mob;

public class Swim<T extends Mob> extends Behavior<T> {
   private final float chance;

   public Swim(final float chance) {
      super(ImmutableMap.of());
      this.chance = chance;
   }

   public static <T extends Mob> boolean shouldSwim(final T mob) {
      return mob.isInWater() && mob.getFluidHeight(FluidTags.WATER) > mob.getFluidJumpThreshold() || mob.isInLava();
   }

   protected boolean checkExtraStartConditions(final ServerLevel level, final Mob body) {
      return shouldSwim(body);
   }

   protected boolean canStillUse(final ServerLevel level, final Mob body, final long timestamp) {
      return this.checkExtraStartConditions(level, body);
   }

   protected void tick(final ServerLevel level, final Mob body, final long timestamp) {
      if (body.getRandom().nextFloat() < this.chance) {
         body.getJumpControl().jump();
      }
   }
}
