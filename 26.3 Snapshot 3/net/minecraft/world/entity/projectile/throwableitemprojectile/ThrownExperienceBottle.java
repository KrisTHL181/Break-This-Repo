package net.minecraft.world.entity.projectile.throwableitemprojectile;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class ThrownExperienceBottle extends ThrowableItemProjectile {
   public ThrownExperienceBottle(final EntityType<? extends ThrownExperienceBottle> type, final Level level) {
      super(type, level);
   }

   public ThrownExperienceBottle(final Level level, final LivingEntity mob, final ItemStack itemStack) {
      super(EntityTypes.EXPERIENCE_BOTTLE, mob, level, itemStack);
   }

   public ThrownExperienceBottle(final Level level, final double x, final double y, final double z, final ItemStack itemStack) {
      super(EntityTypes.EXPERIENCE_BOTTLE, x, y, z, level, itemStack);
   }

   @Override
   protected Item getDefaultItem() {
      return Items.EXPERIENCE_BOTTLE;
   }

   @Override
   protected double getDefaultGravity() {
      return 0.07;
   }

   @Override
   protected void onHit(final HitResult hitResult) {
      super.onHit(hitResult);
      if (this.level() instanceof ServerLevel level) {
         level.levelEvent(2002, this.blockPosition(), -13083194);
         if (!this.isSilent()) {
            level.levelEvent(1053, this.blockPosition(), 0);
         }

         int xpCount = 3 + this.random.nextInt(5) + this.random.nextInt(5);
         if (hitResult instanceof BlockHitResult blockHitResult) {
            Vec3 blockNormalHit = blockHitResult.getDirection().getUnitVec3();
            ExperienceOrb.awardWithDirection(level, hitResult.getLocation(), blockNormalHit, xpCount);
         } else {
            ExperienceOrb.awardWithDirection(level, hitResult.getLocation(), this.getDeltaMovement().scale(-1.0), xpCount);
         }

         this.discard();
      }
   }
}
