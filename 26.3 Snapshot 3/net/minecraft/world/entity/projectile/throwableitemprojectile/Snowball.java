package net.minecraft.world.entity.projectile.throwableitemprojectile;

import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

public class Snowball extends ThrowableItemProjectile {
   public Snowball(final EntityType<? extends Snowball> type, final Level level) {
      super(type, level);
   }

   public Snowball(final Level level, final LivingEntity mob, final ItemStack itemStack) {
      super(EntityTypes.SNOWBALL, mob, level, itemStack);
   }

   public Snowball(final Level level, final double x, final double y, final double z, final ItemStack itemStack) {
      super(EntityTypes.SNOWBALL, x, y, z, level, itemStack);
   }

   @Override
   protected Item getDefaultItem() {
      return Items.SNOWBALL;
   }

   private ParticleOptions getParticle() {
      ItemStack item = this.getItem();
      return item.isEmpty() ? ParticleTypes.ITEM_SNOWBALL : new ItemParticleOption(ParticleTypes.ITEM, ItemStackTemplate.fromNonEmptyStack(item));
   }

   @Override
   public void handleEntityEvent(final byte id) {
      if (id == 3) {
         ParticleOptions particle = this.getParticle();

         for (int i = 0; i < 8; i++) {
            this.level().addParticle(particle, this.getX(), this.getY(), this.getZ(), 0.0, 0.0, 0.0);
         }
      }
   }

   @Override
   protected void onHitEntity(final EntityHitResult hitResult) {
      super.onHitEntity(hitResult);
      Entity entity = hitResult.getEntity();
      int damage = entity instanceof Blaze ? 3 : 0;
      entity.hurt(this.damageSources().thrown(this, this.getOwner()), damage);
   }

   @Override
   protected void onHit(final HitResult hitResult) {
      super.onHit(hitResult);
      if (!this.level().isClientSide()) {
         this.level().broadcastEntityEvent(this, (byte)3);
         this.discard();
      }
   }
}
