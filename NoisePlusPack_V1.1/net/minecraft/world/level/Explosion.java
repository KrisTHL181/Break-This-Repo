package net.minecraft.world.level;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public interface Explosion {
   static DamageSource getDefaultDamageSource(Level p_309890_, @Nullable Entity p_311046_) {
      return p_309890_.damageSources().explosion(p_311046_, getIndirectSourceEntity(p_311046_));
   }

   static @Nullable LivingEntity getIndirectSourceEntity(@Nullable Entity p_362403_) {
      return switch (p_362403_) {
         case PrimedTnt primedtnt -> primedtnt.getOwner();
         case LivingEntity livingentity -> livingentity;
         case Projectile projectile when projectile.getOwner() instanceof LivingEntity livingentity1 -> livingentity1;
         case null, default -> null;
      };
   }

   ServerLevel level();

   Explosion.BlockInteraction getBlockInteraction();

   @Nullable LivingEntity getIndirectSourceEntity();

   @Nullable Entity getDirectSourceEntity();

   float radius();

   Vec3 center();

   boolean canTriggerBlocks();

   boolean shouldAffectBlocklikeEntities();

   enum BlockInteraction {
      KEEP(false),
      DESTROY(true),
      DESTROY_WITH_DECAY(true),
      TRIGGER_BLOCK(false);

      private final boolean shouldAffectBlocklikeEntities;

      BlockInteraction(final boolean p_367015_) {
         this.shouldAffectBlocklikeEntities = p_367015_;
      }

      public boolean shouldAffectBlocklikeEntities() {
         return this.shouldAffectBlocklikeEntities;
      }
   }
}
