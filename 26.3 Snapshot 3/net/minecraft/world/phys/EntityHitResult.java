package net.minecraft.world.phys;

import net.minecraft.world.entity.Entity;

public class EntityHitResult extends HitResult {
   private final Entity entity;

   public EntityHitResult(final Entity entity) {
      this(entity, entity.position());
   }

   public EntityHitResult(final Entity entity, final Vec3 location) {
      super(location);
      this.entity = entity;
   }

   public Entity getEntity() {
      return this.entity;
   }

   @Override
   public HitResult.Type getType() {
      return HitResult.Type.ENTITY;
   }
}
