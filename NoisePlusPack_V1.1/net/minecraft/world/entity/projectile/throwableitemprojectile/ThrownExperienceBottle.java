package net.minecraft.world.entity.projectile.throwableitemprojectile;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
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
   public ThrownExperienceBottle(EntityType<? extends ThrownExperienceBottle> p_459573_, Level p_455505_) {
      super(p_459573_, p_455505_);
   }

   public ThrownExperienceBottle(Level p_451921_, LivingEntity p_456249_, ItemStack p_455780_) {
      super(EntityType.EXPERIENCE_BOTTLE, p_456249_, p_451921_, p_455780_);
   }

   public ThrownExperienceBottle(Level p_450620_, double p_456894_, double p_457462_, double p_459429_, ItemStack p_452098_) {
      super(EntityType.EXPERIENCE_BOTTLE, p_456894_, p_457462_, p_459429_, p_450620_, p_452098_);
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
   protected void onHit(HitResult p_453458_) {
      super.onHit(p_453458_);
      if (this.level() instanceof ServerLevel serverlevel) {
         serverlevel.levelEvent(2002, this.blockPosition(), -13083194);
         int i = 3 + serverlevel.random.nextInt(5) + serverlevel.random.nextInt(5);
         if (p_453458_ instanceof BlockHitResult blockhitresult) {
            Vec3 vec3 = blockhitresult.getDirection().getUnitVec3();
            ExperienceOrb.awardWithDirection(serverlevel, p_453458_.getLocation(), vec3, i);
         } else {
            ExperienceOrb.awardWithDirection(serverlevel, p_453458_.getLocation(), this.getDeltaMovement().scale(-1.0), i);
         }

         this.discard();
      }
   }
}
