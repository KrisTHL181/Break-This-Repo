package net.minecraft.world.entity.projectile;

import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

@FunctionalInterface
public interface ProjectileDeflection {
   ProjectileDeflection NONE = (p_335766_, p_335741_, p_334113_) -> {};
   ProjectileDeflection REVERSE = (p_449740_, p_449741_, p_449742_) -> {
      float f = 170.0F + p_449742_.nextFloat() * 20.0F;
      p_449740_.setDeltaMovement(p_449740_.getDeltaMovement().scale(-0.5));
      p_449740_.setYRot(p_449740_.getYRot() + f);
      p_449740_.yRotO += f;
      p_449740_.needsSync = true;
   };
   ProjectileDeflection AIM_DEFLECT = (p_449737_, p_449738_, p_449739_) -> {
      if (p_449738_ != null) {
         Vec3 vec3 = p_449738_.getLookAngle();
         p_449737_.setDeltaMovement(vec3);
         p_449737_.needsSync = true;
      }
   };
   ProjectileDeflection MOMENTUM_DEFLECT = (p_449743_, p_449744_, p_449745_) -> {
      if (p_449744_ != null) {
         Vec3 vec3 = p_449744_.getDeltaMovement().normalize();
         p_449743_.setDeltaMovement(vec3);
         p_449743_.needsSync = true;
      }
   };

   void deflect(Projectile var1, @Nullable Entity var2, RandomSource var3);
}
