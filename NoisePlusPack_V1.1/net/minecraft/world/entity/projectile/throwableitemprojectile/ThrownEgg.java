package net.minecraft.world.entity.projectile.throwableitemprojectile;

import java.util.Optional;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

public class ThrownEgg extends ThrowableItemProjectile {
   private static final EntityDimensions ZERO_SIZED_DIMENSIONS = EntityDimensions.fixed(0.0F, 0.0F);

   public ThrownEgg(EntityType<? extends ThrownEgg> p_452597_, Level p_452773_) {
      super(p_452597_, p_452773_);
   }

   public ThrownEgg(Level p_452027_, LivingEntity p_452726_, ItemStack p_454220_) {
      super(EntityType.EGG, p_452726_, p_452027_, p_454220_);
   }

   public ThrownEgg(Level p_452375_, double p_450805_, double p_455865_, double p_450207_, ItemStack p_459104_) {
      super(EntityType.EGG, p_450805_, p_455865_, p_450207_, p_452375_, p_459104_);
   }

   @Override
   public void handleEntityEvent(byte p_460800_) {
      if (p_460800_ == 3) {
         double d0 = 0.08;

         for (int i = 0; i < 8; i++) {
            this.level()
               .addParticle(
                  new ItemParticleOption(ParticleTypes.ITEM, this.getItem()),
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

   @Override
   protected void onHitEntity(EntityHitResult p_460143_) {
      super.onHitEntity(p_460143_);
      p_460143_.getEntity().hurt(this.damageSources().thrown(this, this.getOwner()), 0.0F);
   }

   @Override
   protected void onHit(HitResult p_459187_) {
      super.onHit(p_459187_);
      if (!this.level().isClientSide()) {
         if (this.random.nextInt(8) == 0) {
            int i = 1;
            if (this.random.nextInt(32) == 0) {
               i = 4;
            }

            for (int j = 0; j < i; j++) {
               Chicken chicken = EntityType.CHICKEN.create(this.level(), EntitySpawnReason.TRIGGERED);
               if (chicken != null) {
                  chicken.setAge(-24000);
                  chicken.snapTo(this.getX(), this.getY(), this.getZ(), this.getYRot(), 0.0F);
                  Optional.ofNullable(this.getItem().get(DataComponents.CHICKEN_VARIANT))
                     .flatMap(p_460229_ -> p_460229_.unwrap(this.registryAccess()))
                     .ifPresent(chicken::setVariant);
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
