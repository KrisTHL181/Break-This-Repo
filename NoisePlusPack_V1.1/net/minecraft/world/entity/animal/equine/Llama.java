package net.minecraft.world.entity.animal.equine;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import java.util.function.IntFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.util.Util;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityAttachment;
import net.minecraft.world.entity.EntityAttachments;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowParentGoal;
import net.minecraft.world.entity.ai.goal.LlamaFollowCaravanGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.entity.ai.goal.RunAroundLikeCrazyGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.LlamaSpit;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class Llama extends AbstractChestedHorse implements RangedAttackMob {
   private static final int MAX_STRENGTH = 5;
   private static final EntityDataAccessor<Integer> DATA_STRENGTH_ID = SynchedEntityData.defineId(Llama.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Integer> DATA_VARIANT_ID = SynchedEntityData.defineId(Llama.class, EntityDataSerializers.INT);
   private static final EntityDimensions BABY_DIMENSIONS = EntityType.LLAMA
      .getDimensions()
      .withAttachments(EntityAttachments.builder().attach(EntityAttachment.PASSENGER, 0.0F, EntityType.LLAMA.getHeight() - 0.8125F, -0.3F))
      .scale(0.5F);
   boolean didSpit;
   private @Nullable Llama caravanHead;
   private @Nullable Llama caravanTail;

   public Llama(EntityType<? extends Llama> p_454548_, Level p_459437_) {
      super(p_454548_, p_459437_);
      this.getNavigation().setRequiredPathLength(40.0F);
   }

   public boolean isTraderLlama() {
      return false;
   }

   private void setStrength(int p_458785_) {
      this.entityData.set(DATA_STRENGTH_ID, Math.max(1, Math.min(5, p_458785_)));
   }

   private void setRandomStrength(RandomSource p_452169_) {
      int i = p_452169_.nextFloat() < 0.04F ? 5 : 3;
      this.setStrength(1 + p_452169_.nextInt(i));
   }

   public int getStrength() {
      return this.entityData.get(DATA_STRENGTH_ID);
   }

   @Override
   protected void addAdditionalSaveData(ValueOutput p_451141_) {
      super.addAdditionalSaveData(p_451141_);
      p_451141_.store("Variant", Llama.Variant.LEGACY_CODEC, this.getVariant());
      p_451141_.putInt("Strength", this.getStrength());
   }

   @Override
   protected void readAdditionalSaveData(ValueInput p_460090_) {
      this.setStrength(p_460090_.getIntOr("Strength", 0));
      super.readAdditionalSaveData(p_460090_);
      this.setVariant(p_460090_.<Llama.Variant>read("Variant", Llama.Variant.LEGACY_CODEC).orElse(Llama.Variant.DEFAULT));
   }

   @Override
   protected void registerGoals() {
      this.goalSelector.addGoal(0, new FloatGoal(this));
      this.goalSelector.addGoal(1, new RunAroundLikeCrazyGoal(this, 1.2));
      this.goalSelector.addGoal(2, new LlamaFollowCaravanGoal(this, 2.1F));
      this.goalSelector.addGoal(3, new RangedAttackGoal(this, 1.25, 40, 20.0F));
      this.goalSelector.addGoal(3, new PanicGoal(this, 1.2));
      this.goalSelector.addGoal(4, new BreedGoal(this, 1.0));
      this.goalSelector.addGoal(5, new TemptGoal(this, 1.25, p_451588_ -> p_451588_.is(ItemTags.LLAMA_TEMPT_ITEMS), false));
      this.goalSelector.addGoal(6, new FollowParentGoal(this, 1.0));
      this.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 0.7));
      this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 6.0F));
      this.goalSelector.addGoal(9, new RandomLookAroundGoal(this));
      this.targetSelector.addGoal(1, new Llama.LlamaHurtByTargetGoal(this));
      this.targetSelector.addGoal(2, new Llama.LlamaAttackWolfGoal(this));
   }

   public static AttributeSupplier.Builder createAttributes() {
      return createBaseChestedHorseAttributes();
   }

   @Override
   protected void defineSynchedData(SynchedEntityData.Builder p_454681_) {
      super.defineSynchedData(p_454681_);
      p_454681_.define(DATA_STRENGTH_ID, 0);
      p_454681_.define(DATA_VARIANT_ID, 0);
   }

   public Llama.Variant getVariant() {
      return Llama.Variant.byId(this.entityData.get(DATA_VARIANT_ID));
   }

   private void setVariant(Llama.Variant p_454530_) {
      this.entityData.set(DATA_VARIANT_ID, p_454530_.id);
   }

   @Override
   public <T> @Nullable T get(DataComponentType<? extends T> p_460980_) {
      return p_460980_ == DataComponents.LLAMA_VARIANT ? castComponentValue((DataComponentType<T>)p_460980_, this.getVariant()) : super.get(p_460980_);
   }

   @Override
   protected void applyImplicitComponents(DataComponentGetter p_456100_) {
      this.applyImplicitComponentIfPresent(p_456100_, DataComponents.LLAMA_VARIANT);
      super.applyImplicitComponents(p_456100_);
   }

   @Override
   protected <T> boolean applyImplicitComponent(DataComponentType<T> p_451454_, T p_457306_) {
      if (p_451454_ == DataComponents.LLAMA_VARIANT) {
         this.setVariant(castComponentValue(DataComponents.LLAMA_VARIANT, p_457306_));
         return true;
      } else {
         return super.applyImplicitComponent(p_451454_, p_457306_);
      }
   }

   @Override
   public boolean isFood(ItemStack p_455301_) {
      return p_455301_.is(ItemTags.LLAMA_FOOD);
   }

   @Override
   protected boolean handleEating(Player p_455054_, ItemStack p_452745_) {
      int i = 0;
      int j = 0;
      float f = 0.0F;
      boolean flag = false;
      if (p_452745_.is(Items.WHEAT)) {
         i = 10;
         j = 3;
         f = 2.0F;
      } else if (p_452745_.is(Blocks.HAY_BLOCK.asItem())) {
         i = 90;
         j = 6;
         f = 10.0F;
         if (this.isTamed() && this.getAge() == 0 && this.canFallInLove()) {
            flag = true;
            this.setInLove(p_455054_);
         }
      }

      if (this.getHealth() < this.getMaxHealth() && f > 0.0F) {
         this.heal(f);
         flag = true;
      }

      if (this.isBaby() && i > 0) {
         this.level().addParticle(ParticleTypes.HAPPY_VILLAGER, this.getRandomX(1.0), this.getRandomY() + 0.5, this.getRandomZ(1.0), 0.0, 0.0, 0.0);
         if (!this.level().isClientSide()) {
            this.ageUp(i);
            flag = true;
         }
      }

      if (j > 0 && (flag || !this.isTamed()) && this.getTemper() < this.getMaxTemper() && !this.level().isClientSide()) {
         this.modifyTemper(j);
         flag = true;
      }

      if (flag && !this.isSilent()) {
         SoundEvent soundevent = this.getEatingSound();
         if (soundevent != null) {
            this.level()
               .playSound(
                  null,
                  this.getX(),
                  this.getY(),
                  this.getZ(),
                  this.getEatingSound(),
                  this.getSoundSource(),
                  1.0F,
                  1.0F + (this.random.nextFloat() - this.random.nextFloat()) * 0.2F
               );
         }
      }

      return flag;
   }

   @Override
   public boolean isImmobile() {
      return this.isDeadOrDying() || this.isEating();
   }

   @Override
   public @Nullable SpawnGroupData finalizeSpawn(
      ServerLevelAccessor p_453507_, DifficultyInstance p_450964_, EntitySpawnReason p_457523_, @Nullable SpawnGroupData p_458001_
   ) {
      RandomSource randomsource = p_453507_.getRandom();
      this.setRandomStrength(randomsource);
      Llama.Variant llama$variant;
      if (p_458001_ instanceof Llama.LlamaGroupData) {
         llama$variant = ((Llama.LlamaGroupData)p_458001_).variant;
      } else {
         llama$variant = Util.getRandom(Llama.Variant.values(), randomsource);
         p_458001_ = new Llama.LlamaGroupData(llama$variant);
      }

      this.setVariant(llama$variant);
      return super.finalizeSpawn(p_453507_, p_450964_, p_457523_, p_458001_);
   }

   @Override
   protected boolean canPerformRearing() {
      return false;
   }

   @Override
   protected SoundEvent getAngrySound() {
      return SoundEvents.LLAMA_ANGRY;
   }

   @Override
   protected SoundEvent getAmbientSound() {
      return SoundEvents.LLAMA_AMBIENT;
   }

   @Override
   protected SoundEvent getHurtSound(DamageSource p_458384_) {
      return SoundEvents.LLAMA_HURT;
   }

   @Override
   protected SoundEvent getDeathSound() {
      return SoundEvents.LLAMA_DEATH;
   }

   @Override
   protected SoundEvent getEatingSound() {
      return SoundEvents.LLAMA_EAT;
   }

   @Override
   protected void playStepSound(BlockPos p_453659_, BlockState p_458548_) {
      this.playSound(SoundEvents.LLAMA_STEP, 0.15F, 1.0F);
   }

   @Override
   protected void playChestEquipsSound() {
      this.playSound(SoundEvents.LLAMA_CHEST, 1.0F, (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.0F);
   }

   @Override
   public int getInventoryColumns() {
      return this.hasChest() ? this.getStrength() : 0;
   }

   @Override
   public boolean canUseSlot(EquipmentSlot p_450669_) {
      return true;
   }

   @Override
   public int getMaxTemper() {
      return 30;
   }

   @Override
   public boolean canMate(Animal p_456423_) {
      return p_456423_ != this && p_456423_ instanceof Llama && this.canParent() && ((Llama)p_456423_).canParent();
   }

   public @Nullable Llama getBreedOffspring(ServerLevel p_450769_, AgeableMob p_458736_) {
      Llama llama = this.makeNewLlama();
      if (llama != null) {
         this.setOffspringAttributes(p_458736_, llama);
         Llama llama1 = (Llama)p_458736_;
         int i = this.random.nextInt(Math.max(this.getStrength(), llama1.getStrength())) + 1;
         if (this.random.nextFloat() < 0.03F) {
            i++;
         }

         llama.setStrength(i);
         llama.setVariant(this.random.nextBoolean() ? this.getVariant() : llama1.getVariant());
      }

      return llama;
   }

   protected @Nullable Llama makeNewLlama() {
      return EntityType.LLAMA.create(this.level(), EntitySpawnReason.BREEDING);
   }

   private void spit(LivingEntity p_459090_) {
      LlamaSpit llamaspit = new LlamaSpit(this.level(), this);
      double d0 = p_459090_.getX() - this.getX();
      double d1 = p_459090_.getY(0.3333333333333333) - llamaspit.getY();
      double d2 = p_459090_.getZ() - this.getZ();
      double d3 = Math.sqrt(d0 * d0 + d2 * d2) * 0.2F;
      if (this.level() instanceof ServerLevel serverlevel) {
         Projectile.spawnProjectileUsingShoot(llamaspit, serverlevel, ItemStack.EMPTY, d0, d1 + d3, d2, 1.5F, 10.0F);
      }

      if (!this.isSilent()) {
         this.level()
            .playSound(
               null,
               this.getX(),
               this.getY(),
               this.getZ(),
               SoundEvents.LLAMA_SPIT,
               this.getSoundSource(),
               1.0F,
               1.0F + (this.random.nextFloat() - this.random.nextFloat()) * 0.2F
            );
      }

      this.didSpit = true;
   }

   void setDidSpit(boolean p_457758_) {
      this.didSpit = p_457758_;
   }

   @Override
   public boolean causeFallDamage(double p_455330_, float p_456237_, DamageSource p_451412_) {
      int i = this.calculateFallDamage(p_455330_, p_456237_);
      if (i <= 0) {
         return false;
      }

      if (p_455330_ >= 6.0) {
         this.hurt(p_451412_, i);
         this.propagateFallToPassengers(p_455330_, p_456237_, p_451412_);
      }

      this.playBlockFallSound();
      return true;
   }

   public void leaveCaravan() {
      if (this.caravanHead != null) {
         this.caravanHead.caravanTail = null;
      }

      this.caravanHead = null;
   }

   public void joinCaravan(Llama p_460030_) {
      this.caravanHead = p_460030_;
      this.caravanHead.caravanTail = this;
   }

   public boolean hasCaravanTail() {
      return this.caravanTail != null;
   }

   public boolean inCaravan() {
      return this.caravanHead != null;
   }

   public @Nullable Llama getCaravanHead() {
      return this.caravanHead;
   }

   @Override
   protected double followLeashSpeed() {
      return 2.0;
   }

   @Override
   public boolean supportQuadLeash() {
      return false;
   }

   @Override
   protected void followMommy(ServerLevel p_459526_) {
      if (!this.inCaravan() && this.isBaby()) {
         super.followMommy(p_459526_);
      }
   }

   @Override
   public boolean canEatGrass() {
      return false;
   }

   @Override
   public void performRangedAttack(LivingEntity p_458621_, float p_456379_) {
      this.spit(p_458621_);
   }

   @Override
   public Vec3 getLeashOffset() {
      return new Vec3(0.0, 0.75 * this.getEyeHeight(), this.getBbWidth() * 0.5);
   }

   @Override
   public EntityDimensions getDefaultDimensions(Pose p_460011_) {
      return this.isBaby() ? BABY_DIMENSIONS : super.getDefaultDimensions(p_460011_);
   }

   @Override
   protected Vec3 getPassengerAttachmentPoint(Entity p_457942_, EntityDimensions p_457464_, float p_454017_) {
      return getDefaultPassengerAttachmentPoint(this, p_457942_, p_457464_.attachments());
   }

   static class LlamaAttackWolfGoal extends NearestAttackableTargetGoal<Wolf> {
      public LlamaAttackWolfGoal(Llama p_459134_) {
         super(p_459134_, Wolf.class, 16, false, true, (p_460962_, p_450837_) -> !((Wolf)p_460962_).isTame());
      }

      @Override
      protected double getFollowDistance() {
         return super.getFollowDistance() * 0.25;
      }
   }

   static class LlamaGroupData extends AgeableMob.AgeableMobGroupData {
      public final Llama.Variant variant;

      LlamaGroupData(Llama.Variant p_456492_) {
         super(true);
         this.variant = p_456492_;
      }
   }

   static class LlamaHurtByTargetGoal extends HurtByTargetGoal {
      public LlamaHurtByTargetGoal(Llama p_459689_) {
         super(p_459689_);
      }

      @Override
      public boolean canContinueToUse() {
         if (this.mob instanceof Llama llama && llama.didSpit) {
            llama.setDidSpit(false);
            return false;
         } else {
            return super.canContinueToUse();
         }
      }
   }

   public enum Variant implements StringRepresentable {
      CREAMY(0, "creamy"),
      WHITE(1, "white"),
      BROWN(2, "brown"),
      GRAY(3, "gray");

      public static final Llama.Variant DEFAULT = CREAMY;
      private static final IntFunction<Llama.Variant> BY_ID = ByIdMap.continuous(Llama.Variant::getId, values(), ByIdMap.OutOfBoundsStrategy.CLAMP);
      public static final Codec<Llama.Variant> CODEC = StringRepresentable.fromEnum(Llama.Variant::values);
      @Deprecated
      public static final Codec<Llama.Variant> LEGACY_CODEC = Codec.INT.xmap(BY_ID::apply, Llama.Variant::getId);
      public static final StreamCodec<ByteBuf, Llama.Variant> STREAM_CODEC = ByteBufCodecs.idMapper(BY_ID, Llama.Variant::getId);
      final int id;
      private final String name;

      Variant(final int p_454226_, final String p_459299_) {
         this.id = p_454226_;
         this.name = p_459299_;
      }

      public int getId() {
         return this.id;
      }

      public static Llama.Variant byId(int p_457530_) {
         return BY_ID.apply(p_457530_);
      }

      @Override
      public String getSerializedName() {
         return this.name;
      }
   }
}
