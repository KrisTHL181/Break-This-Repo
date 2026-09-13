package net.minecraft.world.entity.projectile.hurtingprojectile;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class SmallFireball extends Fireball {
   public SmallFireball(EntityType<? extends SmallFireball> p_452464_, Level p_451447_) {
      super(p_452464_, p_451447_);
   }

   public SmallFireball(Level p_454571_, LivingEntity p_452734_, Vec3 p_457678_) {
      super(EntityType.SMALL_FIREBALL, p_452734_, p_457678_, p_454571_);
   }

   public SmallFireball(Level p_453440_, double p_455704_, double p_455700_, double p_456620_, Vec3 p_451748_) {
      super(EntityType.SMALL_FIREBALL, p_455704_, p_455700_, p_456620_, p_451748_, p_453440_);
   }

   @Override
   protected void onHitEntity(EntityHitResult p_451427_) {
      super.onHitEntity(p_451427_);
      if (this.level() instanceof ServerLevel serverlevel) {
         Entity entity1 = p_451427_.getEntity();
         Entity $$4 = this.getOwner();
         int $$5 = entity1.getRemainingFireTicks();
         entity1.igniteForSeconds(5.0F);
         DamageSource $$6 = this.damageSources().fireball(this, $$4);
         if (!entity1.hurtServer(serverlevel, $$6, 5.0F)) {
            entity1.setRemainingFireTicks($$5);
         } else {
            EnchantmentHelper.doPostAttackEffects(serverlevel, entity1, $$6);
         }
      }
   }

   @Override
   protected void onHitBlock(BlockHitResult p_452526_) {
      super.onHitBlock(p_452526_);
      if (this.level() instanceof ServerLevel serverlevel) {
         Entity entity = this.getOwner();
         if (!(entity instanceof Mob) || serverlevel.getGameRules().get(GameRules.MOB_GRIEFING)) {
            BlockPos blockpos = p_452526_.getBlockPos().relative(p_452526_.getDirection());
            if (this.level().isEmptyBlock(blockpos)) {
               this.level().setBlockAndUpdate(blockpos, BaseFireBlock.getState(this.level(), blockpos));
            }
         }
      }
   }

   @Override
   protected void onHit(HitResult p_459213_) {
      super.onHit(p_459213_);
      if (!this.level().isClientSide()) {
         this.discard();
      }
   }
}
