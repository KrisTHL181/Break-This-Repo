package net.minecraft.world.entity;

import java.util.function.Consumer;
import net.minecraft.world.level.block.BaseFireBlock;

public enum InsideBlockEffectType {
   FREEZE(p_395001_ -> {
      p_395001_.setIsInPowderSnow(true);
      if (p_395001_.canFreeze()) {
         p_395001_.setTicksFrozen(Math.min(p_395001_.getTicksRequiredToFreeze(), p_395001_.getTicksFrozen() + 1));
      }
   }),
   CLEAR_FREEZE(Entity::clearFreeze),
   FIRE_IGNITE(BaseFireBlock::fireIgnite),
   LAVA_IGNITE(Entity::lavaIgnite),
   EXTINGUISH(Entity::clearFire);

   private final Consumer<Entity> effect;

   InsideBlockEffectType(final Consumer<Entity> p_397759_) {
      this.effect = p_397759_;
   }

   public Consumer<Entity> effect() {
      return this.effect;
   }
}
