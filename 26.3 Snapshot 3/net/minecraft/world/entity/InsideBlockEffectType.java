package net.minecraft.world.entity;

import java.util.function.Consumer;
import net.minecraft.world.level.block.BaseFireBlock;

public enum InsideBlockEffectType {
   FREEZE(entity -> {
      entity.setIsInPowderSnow(true);
      if (entity.canFreeze()) {
         entity.setTicksFrozen(Math.min(entity.getTicksRequiredToFreeze(), entity.getTicksFrozen() + 1));
      }
   }),
   CLEAR_FREEZE(Entity::clearFreeze),
   FIRE_IGNITE(BaseFireBlock::fireIgnite),
   LAVA_IGNITE(Entity::lavaIgnite),
   EXTINGUISH(Entity::clearFire);

   private final Consumer<Entity> effect;

   InsideBlockEffectType(final Consumer<Entity> effect) {
      this.effect = effect;
   }

   public Consumer<Entity> effect() {
      return this.effect;
   }
}
