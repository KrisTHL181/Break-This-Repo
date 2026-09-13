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

   public VehicleEntity(final EntityType<?> type, final Level level) {
      super(type, level);
   }

   @Override
   public boolean hurtClient(final DamageSource source) {
      return true;
   }

   @Override
   public boolean hurtServer(final ServerLevel level, final DamageSource source, final float damage) {
      if (this.isRemoved()) {
         return true;
      }

      if (this.isInvulnerableToBase(source)) {
         return false;
      }

      this.setHurtDir(-this.getHurtDir());
      this.setHurtTime(10);
      this.markHurt();
      this.setDamage(this.getDamage() + damage * 10.0F);
      this.gameEvent(GameEvent.ENTITY_DAMAGE, source.getEntity());
      boolean creativePlayer = source.getEntity() instanceof Player player && player.getAbilities().instabuild;
      if ((creativePlayer || !(this.getDamage() > 40.0F)) && !this.shouldSourceDestroy(source)) {
         if (creativePlayer) {
            this.discard();
         }
      } else {
         this.destroy(level, source);
      }

      return true;
   }

   protected boolean shouldSourceDestroy(final DamageSource source) {
      return false;
   }

   @Override
   public boolean ignoreExplosion(final Explosion explosion) {
      return explosion.getIndirectSourceEntity() instanceof Mob && !explosion.level().getGameRules().get(GameRules.MOB_GRIEFING);
   }

   public void destroy(final ServerLevel level, final Item dropItem) {
      this.kill(level);
      if (level.getGameRules().get(GameRules.ENTITY_DROPS)) {
         ItemStack itemStack = new ItemStack(dropItem);
         itemStack.set(DataComponents.CUSTOM_NAME, this.getCustomName());
         this.spawnAtLocation(level, itemStack);
      }
   }

   @Override
   protected void defineSynchedData(final SynchedEntityData.Builder entityData) {
      entityData.define(DATA_ID_HURT, 0);
      entityData.define(DATA_ID_HURTDIR, 1);
      entityData.define(DATA_ID_DAMAGE, 0.0F);
   }

   public void setHurtTime(final int hurtTime) {
      this.entityData.set(DATA_ID_HURT, hurtTime);
   }

   public void setHurtDir(final int hurtDir) {
      this.entityData.set(DATA_ID_HURTDIR, hurtDir);
   }

   public void setDamage(final float damage) {
      this.entityData.set(DATA_ID_DAMAGE, damage);
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

   protected void destroy(final ServerLevel level, final DamageSource source) {
      this.destroy(level, this.getDropItem());
   }

   @Override
   public int getDimensionChangingDelay() {
      return 10;
   }

   protected abstract Item getDropItem();
}
