package net.minecraft.world.entity.vehicle;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gamerules.GameRules;

public abstract class VehicleEntity extends Entity {
   protected static final EntityDataAccessor<Integer> DATA_ID_HURT = SynchedEntityData.defineId(VehicleEntity.class, EntityDataSerializers.INT);
   protected static final EntityDataAccessor<Integer> DATA_ID_HURTDIR = SynchedEntityData.defineId(VehicleEntity.class, EntityDataSerializers.INT);
   protected static final EntityDataAccessor<Float> DATA_ID_DAMAGE = SynchedEntityData.defineId(VehicleEntity.class, EntityDataSerializers.FLOAT);

   public VehicleEntity(EntityType<?> p_310168_, Level p_309578_) {
      super(p_310168_, p_309578_);
   }

   @Override
   public boolean hurtClient(DamageSource p_364732_) {
      return true;
   }

   @Override
   public boolean hurtServer(ServerLevel p_369362_, DamageSource p_369351_, float p_361075_) {
      if (this.isRemoved()) {
         return true;
      }

      if (this.isInvulnerableToBase(p_369351_)) {
         return false;
      }

      this.setHurtDir(-this.getHurtDir());
      this.setHurtTime(10);
      this.markHurt();
      this.setDamage(this.getDamage() + p_361075_ * 10.0F);
      this.gameEvent(GameEvent.ENTITY_DAMAGE, p_369351_.getEntity());
      boolean flag = p_369351_.getEntity() instanceof Player player && player.getAbilities().instabuild;
      if ((flag || !(this.getDamage() > 40.0F)) && !this.shouldSourceDestroy(p_369351_)) {
         if (flag) {
            this.discard();
         }
      } else {
         this.destroy(p_369362_, p_369351_);
      }

      return true;
   }

   protected boolean shouldSourceDestroy(DamageSource p_309621_) {
      return false;
   }

   @Override
   public boolean ignoreExplosion(Explosion p_366232_) {
      return p_366232_.getIndirectSourceEntity() instanceof Mob && !p_366232_.level().getGameRules().get(GameRules.MOB_GRIEFING);
   }

   public void destroy(ServerLevel p_367176_, Item p_313028_) {
      this.kill(p_367176_);
      if (p_367176_.getGameRules().get(GameRules.ENTITY_DROPS)) {
         ItemStack itemstack = new ItemStack(p_313028_);
         itemstack.set(DataComponents.CUSTOM_NAME, this.getCustomName());
         this.spawnAtLocation(p_367176_, itemstack);
      }
   }

   @Override
   protected void defineSynchedData(SynchedEntityData.Builder p_332479_) {
      p_332479_.define(DATA_ID_HURT, 0);
      p_332479_.define(DATA_ID_HURTDIR, 1);
      p_332479_.define(DATA_ID_DAMAGE, 0.0F);
   }

   public void setHurtTime(int p_312621_) {
      this.entityData.set(DATA_ID_HURT, p_312621_);
   }

   public void setHurtDir(int p_312074_) {
      this.entityData.set(DATA_ID_HURTDIR, p_312074_);
   }

   public void setDamage(float p_313007_) {
      this.entityData.set(DATA_ID_DAMAGE, p_313007_);
   }

   public float getDamage() {
      return this.entityData.get(DATA_ID_DAMAGE);
   }

   public int getHurtTime() {
      return this.entityData.get(DATA_ID_HURT);
   }

   public int getHurtDir() {
      return this.entityData.get(DATA_ID_HURTDIR);
   }

   protected void destroy(ServerLevel p_365692_, DamageSource p_312900_) {
      this.destroy(p_365692_, this.getDropItem());
   }

   @Override
   public int getDimensionChangingDelay() {
      return 10;
   }

   protected abstract Item getDropItem();
}
