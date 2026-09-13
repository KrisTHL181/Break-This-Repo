package net.minecraft.world.entity.monster.skeleton;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Shearable;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import org.jspecify.annotations.Nullable;

public class Bogged extends AbstractSkeleton implements Shearable {
   private static final EntityDataAccessor<Boolean> DATA_SHEARED = SynchedEntityData.defineId(Bogged.class, EntityDataSerializers.BOOLEAN);
   private static final String SHEARED_TAG_NAME = "sheared";
   private static final boolean DEFAULT_SHEARED = false;

   public static AttributeSupplier.Builder createAttributes() {
      return AbstractSkeleton.createAttributes().add(Attributes.MAX_HEALTH, 16.0);
   }

   public Bogged(EntityType<? extends Bogged> p_460563_, Level p_457706_) {
      super(p_460563_, p_457706_);
   }

   @Override
   protected void defineSynchedData(SynchedEntityData.Builder p_452459_) {
      super.defineSynchedData(p_452459_);
      p_452459_.define(DATA_SHEARED, false);
   }

   @Override
   protected void addAdditionalSaveData(ValueOutput p_455849_) {
      super.addAdditionalSaveData(p_455849_);
      p_455849_.putBoolean("sheared", this.isSheared());
   }

   @Override
   protected void readAdditionalSaveData(ValueInput p_458358_) {
      super.readAdditionalSaveData(p_458358_);
      this.setSheared(p_458358_.getBooleanOr("sheared", false));
   }

   public boolean isSheared() {
      return this.entityData.get(DATA_SHEARED);
   }

   public void setSheared(boolean p_456183_) {
      this.entityData.set(DATA_SHEARED, p_456183_);
   }

   @Override
   protected InteractionResult mobInteract(Player p_453508_, InteractionHand p_459904_) {
      ItemStack itemstack = p_453508_.getItemInHand(p_459904_);
      if (itemstack.is(Items.SHEARS) && this.readyForShearing()) {
         if (this.level() instanceof ServerLevel serverlevel) {
            this.shear(serverlevel, SoundSource.PLAYERS, itemstack);
            this.gameEvent(GameEvent.SHEAR, p_453508_);
            itemstack.hurtAndBreak(1, p_453508_, p_459904_.asEquipmentSlot());
         }

         return InteractionResult.SUCCESS;
      } else {
         return super.mobInteract(p_453508_, p_459904_);
      }
   }

   @Override
   protected SoundEvent getAmbientSound() {
      return SoundEvents.BOGGED_AMBIENT;
   }

   @Override
   protected SoundEvent getHurtSound(DamageSource p_451367_) {
      return SoundEvents.BOGGED_HURT;
   }

   @Override
   protected SoundEvent getDeathSound() {
      return SoundEvents.BOGGED_DEATH;
   }

   @Override
   protected SoundEvent getStepSound() {
      return SoundEvents.BOGGED_STEP;
   }

   @Override
   protected AbstractArrow getArrow(ItemStack p_451963_, float p_451518_, @Nullable ItemStack p_458834_) {
      AbstractArrow abstractarrow = super.getArrow(p_451963_, p_451518_, p_458834_);
      if (abstractarrow instanceof Arrow arrow) {
         arrow.addEffect(new MobEffectInstance(MobEffects.POISON, 100));
      }

      return abstractarrow;
   }

   @Override
   protected int getHardAttackInterval() {
      return 50;
   }

   @Override
   protected int getAttackInterval() {
      return 70;
   }

   @Override
   public void shear(ServerLevel p_454864_, SoundSource p_459798_, ItemStack p_453652_) {
      p_454864_.playSound(null, this, SoundEvents.BOGGED_SHEAR, p_459798_, 1.0F, 1.0F);
      this.spawnShearedMushrooms(p_454864_, p_453652_);
      this.setSheared(true);
   }

   private void spawnShearedMushrooms(ServerLevel p_454731_, ItemStack p_453578_) {
      this.dropFromShearingLootTable(
         p_454731_, BuiltInLootTables.BOGGED_SHEAR, p_453578_, (p_456269_, p_457387_) -> this.spawnAtLocation(p_456269_, p_457387_, this.getBbHeight())
      );
   }

   @Override
   public boolean readyForShearing() {
      return !this.isSheared() && this.isAlive();
   }
}
