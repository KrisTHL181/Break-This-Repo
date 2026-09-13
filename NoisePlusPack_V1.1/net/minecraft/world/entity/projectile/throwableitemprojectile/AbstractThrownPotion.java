package net.minecraft.world.entity.projectile.throwableitemprojectile;

import it.unimi.dsi.fastutil.doubles.DoubleDoubleImmutablePair;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.axolotl.Axolotl;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractCandleBlock;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public abstract class AbstractThrownPotion extends ThrowableItemProjectile {
   public static final double SPLASH_RANGE = 4.0;
   protected static final double SPLASH_RANGE_SQ = 16.0;
   public static final Predicate<LivingEntity> WATER_SENSITIVE_OR_ON_FIRE = p_459971_ -> p_459971_.isSensitiveToWater() || p_459971_.isOnFire();

   public AbstractThrownPotion(EntityType<? extends AbstractThrownPotion> p_458779_, Level p_458628_) {
      super(p_458779_, p_458628_);
   }

   public AbstractThrownPotion(EntityType<? extends AbstractThrownPotion> p_455811_, Level p_454900_, LivingEntity p_455184_, ItemStack p_450143_) {
      super(p_455811_, p_455184_, p_454900_, p_450143_);
   }

   public AbstractThrownPotion(
      EntityType<? extends AbstractThrownPotion> p_451968_, Level p_456304_, double p_460675_, double p_458239_, double p_451651_, ItemStack p_450979_
   ) {
      super(p_451968_, p_460675_, p_458239_, p_451651_, p_456304_, p_450979_);
   }

   @Override
   protected double getDefaultGravity() {
      return 0.05;
   }

   @Override
   protected void onHitBlock(BlockHitResult p_455381_) {
      super.onHitBlock(p_455381_);
      if (!this.level().isClientSide()) {
         ItemStack itemstack = this.getItem();
         Direction direction = p_455381_.getDirection();
         BlockPos blockpos = p_455381_.getBlockPos();
         BlockPos blockpos1 = blockpos.relative(direction);
         PotionContents potioncontents = itemstack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
         if (potioncontents.is(Potions.WATER)) {
            this.dowseFire(blockpos1);
            this.dowseFire(blockpos1.relative(direction.getOpposite()));

            for (Direction direction1 : Direction.Plane.HORIZONTAL) {
               this.dowseFire(blockpos1.relative(direction1));
            }
         }
      }
   }

   @Override
   protected void onHit(HitResult p_454720_) {
      super.onHit(p_454720_);
      if (this.level() instanceof ServerLevel serverlevel) {
         ItemStack itemstack = this.getItem();
         PotionContents potioncontents = itemstack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
         if (potioncontents.is(Potions.WATER)) {
            this.onHitAsWater(serverlevel);
         } else if (potioncontents.hasEffects()) {
            this.onHitAsPotion(serverlevel, itemstack, p_454720_);
         }

         int i = potioncontents.potion().isPresent() && potioncontents.potion().get().value().hasInstantEffects() ? 2007 : 2002;
         serverlevel.levelEvent(i, this.blockPosition(), potioncontents.getColor());
         this.discard();
      }
   }

   private void onHitAsWater(ServerLevel p_459102_) {
      AABB aabb = this.getBoundingBox().inflate(4.0, 2.0, 4.0);

      for (LivingEntity livingentity : this.level().getEntitiesOfClass(LivingEntity.class, aabb, WATER_SENSITIVE_OR_ON_FIRE)) {
         double d0 = this.distanceToSqr(livingentity);
         if (d0 < 16.0) {
            if (livingentity.isSensitiveToWater()) {
               livingentity.hurtServer(p_459102_, this.damageSources().indirectMagic(this, this.getOwner()), 1.0F);
            }

            if (livingentity.isOnFire() && livingentity.isAlive()) {
               livingentity.extinguishFire();
            }
         }
      }

      for (Axolotl axolotl : this.level().getEntitiesOfClass(Axolotl.class, aabb)) {
         axolotl.rehydrate();
      }
   }

   protected abstract void onHitAsPotion(ServerLevel var1, ItemStack var2, HitResult var3);

   private void dowseFire(BlockPos p_457390_) {
      BlockState blockstate = this.level().getBlockState(p_457390_);
      if (blockstate.is(BlockTags.FIRE)) {
         this.level().destroyBlock(p_457390_, false, this);
      } else if (AbstractCandleBlock.isLit(blockstate)) {
         AbstractCandleBlock.extinguish(null, blockstate, this.level(), p_457390_);
      } else if (CampfireBlock.isLitCampfire(blockstate)) {
         this.level().levelEvent(null, 1009, p_457390_, 0);
         CampfireBlock.dowse(this.getOwner(), this.level(), p_457390_, blockstate);
         this.level().setBlockAndUpdate(p_457390_, blockstate.setValue(CampfireBlock.LIT, false));
      }
   }

   @Override
   public DoubleDoubleImmutablePair calculateHorizontalHurtKnockbackDirection(LivingEntity p_452648_, DamageSource p_454552_) {
      double d0 = p_452648_.position().x - this.position().x;
      double d1 = p_452648_.position().z - this.position().z;
      return DoubleDoubleImmutablePair.of(d0, d1);
   }
}
