package net.minecraft.world.entity.projectile;

import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

@FunctionalInterface
public interface ProjectileDeflection {
   ProjectileDeflection NONE = (projectile, entity, random) -> {};
   ProjectileDeflection REVERSE = (projectile, entity, random) -> {
      float rotation = 170.0F + random.nextFloat() * 20.0F;
      projectile.setDeltaMovement(projectile.getDeltaMovement().scale(-0.5));
      projectile.setYRot(projectile.getYRot() + rotation);
      projectile.yRotO += rotation;
      projectile.needsSync = true;
   };
   ProjectileDeflection AIM_DEFLECT = (projectile, entity, random) -> {
      if (entity != null) {
         Vec3 lookAngle = entity.getLookAngle();
         projectile.setDeltaMovement(lookAngle);
         projectile.needsSync = true;
      }
   };
   ProjectileDeflection MOMENTUM_DEFLECT = (projectile, entity, random) -> {
      if (entity != null) {
         Vec3 movement = entity.getDeltaMovement().normalize();
         projectile.setDeltaMovement(movement);
         projectile.needsSync = true;
      }
   };

   void deflect(final Projectile projectile, final @Nullable Entity entity, final RandomSource random);
}
