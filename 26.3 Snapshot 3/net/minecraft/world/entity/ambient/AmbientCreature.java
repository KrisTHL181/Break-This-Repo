package net.minecraft.world.entity.ambient;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;

public abstract class AmbientCreature extends Mob {
   protected AmbientCreature(final EntityType<? extends AmbientCreature> type, final Level level) {
      super(type, level);
   }

   @Override
   public boolean canBeLeashed() {
      return false;
   }
}
