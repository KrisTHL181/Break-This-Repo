package net.minecraft.world.entity.monster.skeleton;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.piglin.AbstractPiglin;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.pathfinder.PathType;
import org.jspecify.annotations.Nullable;

public class WitherSkeleton extends AbstractSkeleton {
   public WitherSkeleton(EntityType<? extends WitherSkeleton> p_459870_, Level p_458620_) {
      super(p_459870_, p_458620_);
      this.setPathfindingMalus(PathType.LAVA, 8.0F);
   }

   @Override
   protected void registerGoals() {
      this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, AbstractPiglin.class, true));
      super.registerGoals();
   }

   @Override
   protected SoundEvent getAmbientSound() {
      return SoundEvents.WITHER_SKELETON_AMBIENT;
   }

   @Override
   protected SoundEvent getHurtSound(DamageSource p_450452_) {
      return SoundEvents.WITHER_SKELETON_HURT;
   }

   @Override
   protected SoundEvent getDeathSound() {
      return SoundEvents.WITHER_SKELETON_DEATH;
   }

   @Override
   SoundEvent getStepSound() {
      return SoundEvents.WITHER_SKELETON_STEP;
   }

   @Override
   public TagKey<Item> getPreferredWeaponType() {
      return null;
   }

   @Override
   public boolean canHoldItem(ItemStack p_456052_) {
      return !p_456052_.is(ItemTags.WITHER_SKELETON_DISLIKED_WEAPONS) && super.canHoldItem(p_456052_);
   }

   @Override
   protected void populateDefaultEquipmentSlots(RandomSource p_453758_, DifficultyInstance p_454460_) {
      this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.STONE_SWORD));
   }

   @Override
   protected void populateDefaultEquipmentEnchantments(ServerLevelAccessor p_459769_, RandomSource p_459468_, DifficultyInstance p_459597_) {
   }

   @Override
   public @Nullable SpawnGroupData finalizeSpawn(
      ServerLevelAccessor p_454816_, DifficultyInstance p_458720_, EntitySpawnReason p_450710_, @Nullable SpawnGroupData p_451122_
   ) {
      SpawnGroupData spawngroupdata = super.finalizeSpawn(p_454816_, p_458720_, p_450710_, p_451122_);
      this.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(4.0);
      this.reassessWeaponGoal();
      return spawngroupdata;
   }

   @Override
   public boolean doHurtTarget(ServerLevel p_450977_, Entity p_450859_) {
      if (!super.doHurtTarget(p_450977_, p_450859_)) {
         return false;
      }

      if (p_450859_ instanceof LivingEntity) {
         ((LivingEntity)p_450859_).addEffect(new MobEffectInstance(MobEffects.WITHER, 200), this);
      }

      return true;
   }

   @Override
   protected AbstractArrow getArrow(ItemStack p_450780_, float p_459667_, @Nullable ItemStack p_455683_) {
      AbstractArrow abstractarrow = super.getArrow(p_450780_, p_459667_, p_455683_);
      abstractarrow.igniteForSeconds(100.0F);
      return abstractarrow;
   }

   @Override
   public boolean canBeAffected(MobEffectInstance p_459773_) {
      return p_459773_.is(MobEffects.WITHER) ? false : super.canBeAffected(p_459773_);
   }
}
