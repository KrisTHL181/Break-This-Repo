package net.minecraft.world.entity.projectile.throwableitemprojectile;

import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

public class Snowball extends ThrowableItemProjectile {
   public Snowball(EntityType<? extends Snowball> p_453581_, Level p_460903_) {
      super(p_453581_, p_460903_);
   }

   public Snowball(Level p_454836_, LivingEntity p_458340_, ItemStack p_456258_) {
      super(EntityType.SNOWBALL, p_458340_, p_454836_, p_456258_);
   }

   public Snowball(Level p_457788_, double p_455743_, double p_457695_, double p_452739_, ItemStack p_457593_) {
      super(EntityType.SNOWBALL, p_455743_, p_457695_, p_452739_, p_457788_, p_457593_);
   }

   @Override
   protected Item getDefaultItem() {
      return Items.SNOWBALL;
   }

   private ParticleOptions getParticle() {
      ItemStack itemstack = this.getItem();
      return itemstack.isEmpty() ? ParticleTypes.ITEM_SNOWBALL : new ItemParticleOption(ParticleTypes.ITEM, itemstack);
   }

   @Override
   public void handleEntityEvent(byte p_455160_) {
      if (p_455160_ == 3) {
         ParticleOptions particleoptions = this.getParticle();

         for (int i = 0; i < 8; i++) {
            this.level().addParticle(particleoptions, this.getX(), this.getY(), this.getZ(), 0.0, 0.0, 0.0);
         }
      }
   }

   @Override
   protected void onHitEntity(EntityHitResult p_458051_) {
      super.onHitEntity(p_458051_);
      Entity entity = p_458051_.getEntity();
      int i = entity instanceof Blaze ? 3 : 0;
      entity.hurt(this.damageSources().thrown(this, this.getOwner()), i);
   }

   @Override
   protected void onHit(HitResult p_456066_) {
      super.onHit(p_456066_);
      if (!this.level().isClientSide()) {
         this.level().broadcastEntityEvent(this, (byte)3);
         this.discard();
      }
   }
}
