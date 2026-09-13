package net.minecraft.world.entity.projectile.hurtingprojectile;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class LargeFireball extends Fireball {
   private static final byte DEFAULT_EXPLOSION_POWER = 1;
   private int explosionPower = 1;

   public LargeFireball(EntityType<? extends LargeFireball> p_460788_, Level p_451825_) {
      super(p_460788_, p_451825_);
   }

   public LargeFireball(Level p_458415_, LivingEntity p_457516_, Vec3 p_456197_, int p_452800_) {
      super(EntityType.FIREBALL, p_457516_, p_456197_, p_458415_);
      this.explosionPower = p_452800_;
   }

   @Override
   protected void onHit(HitResult p_451111_) {
      super.onHit(p_451111_);
      if (this.level() instanceof ServerLevel serverlevel) {
         boolean flag = serverlevel.getGameRules().get(GameRules.MOB_GRIEFING);
         this.level().explode(this, this.getX(), this.getY(), this.getZ(), this.explosionPower, flag, Level.ExplosionInteraction.MOB);
         this.discard();
      }
   }

   @Override
   protected void onHitEntity(EntityHitResult p_451837_) {
      super.onHitEntity(p_451837_);
      if (this.level() instanceof ServerLevel serverlevel) {
         Entity entity1 = p_451837_.getEntity();
         Entity $$4 = this.getOwner();
         DamageSource $$5 = this.damageSources().fireball(this, $$4);
         entity1.hurtServer(serverlevel, $$5, 6.0F);
         EnchantmentHelper.doPostAttackEffects(serverlevel, entity1, $$5);
      }
   }

   @Override
   protected void addAdditionalSaveData(ValueOutput p_453802_) {
      super.addAdditionalSaveData(p_453802_);
      p_453802_.putByte("ExplosionPower", (byte)this.explosionPower);
   }

   @Override
   protected void readAdditionalSaveData(ValueInput p_452763_) {
      super.readAdditionalSaveData(p_452763_);
      this.explosionPower = p_452763_.getByteOr("ExplosionPower", (byte)1);
   }
}
