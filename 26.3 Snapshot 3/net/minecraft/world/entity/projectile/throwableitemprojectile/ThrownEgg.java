package net.minecraft.world.entity.projectile.throwableitemprojectile;

import java.util.Optional;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

public class ThrownEgg extends ThrowableItemProjectile {
   private static final EntityDimensions ZERO_SIZED_DIMENSIONS = EntityDimensions.fixed(0.0F, 0.0F);

   public ThrownEgg(final EntityType<? extends ThrownEgg> type, final Level level) {
      super(type, level);
   }

   public ThrownEgg(final Level level, final LivingEntity mob, final ItemStack itemStack) {
      super(EntityTypes.EGG, mob, level, itemStack);
   }

   public ThrownEgg(final Level level, final double x, final double y, final double z, final ItemStack itemStack) {
      super(EntityTypes.EGG, x, y, z, level, itemStack);
   }

   @Override
   public void handleEntityEvent(final byte id) {
      if (id == 3) {
         ItemStack item = this.getItem();
         if (!item.isEmpty()) {
            ItemParticleOption breakParticle = new ItemParticleOption(ParticleTypes.ITEM, ItemStackTemplate.fromNonEmptyStack(item));

            for (int i = 0; i < 8; i++) {
               this.level()
                  .addParticle(
                     breakParticle,
                     this.getX(),
                     this.getY(),
                     this.getZ(),
                     (this.random.nextFloat() - 0.5) * 0.08,
                     (this.random.nextFloat() - 0.5) * 0.08,
                     (this.random.nextFloat() - 0.5) * 0.08
                  );
            }
         }
      }
   }

   @Override
   protected void onHitEntity(final EntityHitResult hitResult) {
      super.onHitEntity(hitResult);
      hitResult.getEntity().hurt(this.damageSources().thrown(this, this.getOwner()), 0.0F);
   }

   @Override
   protected void onHit(final HitResult hitResult) {
      super.onHit(hitResult);
      if (!this.level().isClientSide()) {
         if (this.random.nextInt(8) == 0) {
            int count = 1;
            if (this.random.nextInt(32) == 0) {
               count = 4;
            }

            for (int i = 0; i < count; i++) {
               Chicken chicken = EntityTypes.CHICKEN.create(this.level(), EntitySpawnReason.TRIGGERED);
               if (chicken != null) {
                  chicken.setAge(-24000);
                  chicken.snapTo(this.getX(), this.getY(), this.getZ(), this.getYRot(), 0.0F);
                  Optional.ofNullable(this.getItem().get(DataComponents.CHICKEN_VARIANT)).ifPresent(chicken::setVariant);
                  if (!chicken.fudgePositionAfterSizeChange(ZERO_SIZED_DIMENSIONS)) {
                     break;
                  }

                  this.level().addFreshEntity(chicken);
               }
            }
         }

         this.level().broadcastEntityEvent(this, (byte)3);
         this.discard();
      }
   }

   @Override
   protected Item getDefaultItem() {
      return Items.EGG;
   }
}
