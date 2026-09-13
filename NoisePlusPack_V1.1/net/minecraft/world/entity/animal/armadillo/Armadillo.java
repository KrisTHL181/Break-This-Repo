package net.minecraft.world.entity.animal.armadillo;

import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import io.netty.buffer.ByteBuf;
import java.util.function.IntFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.util.TimeUtil;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.BodyRotationControl;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import org.jspecify.annotations.Nullable;

public class Armadillo extends Animal {
   public static final float BABY_SCALE = 0.6F;
   public static final float MAX_HEAD_ROTATION_EXTENT = 32.5F;
   public static final int SCARE_CHECK_INTERVAL = 80;
   private static final double SCARE_DISTANCE_HORIZONTAL = 7.0;
   private static final double SCARE_DISTANCE_VERTICAL = 2.0;
   private static final EntityDataAccessor<Armadillo.ArmadilloState> ARMADILLO_STATE = SynchedEntityData.defineId(
      Armadillo.class, EntityDataSerializers.ARMADILLO_STATE
   );
   private long inStateTicks = 0L;
   public final AnimationState rollOutAnimationState = new AnimationState();
   public final AnimationState rollUpAnimationState = new AnimationState();
   public final AnimationState peekAnimationState = new AnimationState();
   private int scuteTime;
   private boolean peekReceivedClient = false;

   public Armadillo(EntityType<? extends Animal> p_331987_, Level p_331498_) {
      super(p_331987_, p_331498_);
      this.getNavigation().setCanFloat(true);
      this.scuteTime = this.pickNextScuteDropTime();
   }

   @Override
   public @Nullable AgeableMob getBreedOffspring(ServerLevel p_330674_, AgeableMob p_330373_) {
      return EntityType.ARMADILLO.create(p_330674_, EntitySpawnReason.BREEDING);
   }

   public static AttributeSupplier.Builder createAttributes() {
      return Animal.createAnimalAttributes().add(Attributes.MAX_HEALTH, 12.0).add(Attributes.MOVEMENT_SPEED, 0.14);
   }

   @Override
   protected void defineSynchedData(SynchedEntityData.Builder p_335155_) {
      super.defineSynchedData(p_335155_);
      p_335155_.define(ARMADILLO_STATE, Armadillo.ArmadilloState.IDLE);
   }

   public boolean isScared() {
      return this.entityData.get(ARMADILLO_STATE) != Armadillo.ArmadilloState.IDLE;
   }

   public boolean shouldHideInShell() {
      return this.getState().shouldHideInShell(this.inStateTicks);
   }

   public boolean shouldSwitchToScaredState() {
      return this.getState() == Armadillo.ArmadilloState.ROLLING && this.inStateTicks > Armadillo.ArmadilloState.ROLLING.animationDuration();
   }

   public Armadillo.ArmadilloState getState() {
      return this.entityData.get(ARMADILLO_STATE);
   }

   public void switchToState(Armadillo.ArmadilloState p_330881_) {
      this.entityData.set(ARMADILLO_STATE, p_330881_);
   }

   @Override
   public void onSyncedDataUpdated(EntityDataAccessor<?> p_328821_) {
      if (ARMADILLO_STATE.equals(p_328821_)) {
         this.inStateTicks = 0L;
      }

      super.onSyncedDataUpdated(p_328821_);
   }

   @Override
   protected Brain.Provider<Armadillo> brainProvider() {
      return ArmadilloAi.brainProvider();
   }

   @Override
   protected Brain<?> makeBrain(Dynamic<?> p_334417_) {
      return ArmadilloAi.makeBrain(this.brainProvider().makeBrain(p_334417_));
   }

   @Override
   protected void customServerAiStep(ServerLevel p_360882_) {
      ProfilerFiller profilerfiller = Profiler.get();
      profilerfiller.push("armadilloBrain");
      ((Brain<Armadillo>)this.brain).tick(p_360882_, this);
      profilerfiller.pop();
      profilerfiller.push("armadilloActivityUpdate");
      ArmadilloAi.updateActivity(this);
      profilerfiller.pop();
      if (this.isAlive() && --this.scuteTime <= 0 && this.shouldDropLoot(p_360882_)) {
         if (this.dropFromGiftLootTable(p_360882_, BuiltInLootTables.ARMADILLO_SHED, this::spawnAtLocation)) {
            this.playSound(SoundEvents.ARMADILLO_SCUTE_DROP, 1.0F, (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.0F);
            this.gameEvent(GameEvent.ENTITY_PLACE);
         }

         this.scuteTime = this.pickNextScuteDropTime();
      }

      super.customServerAiStep(p_360882_);
   }

   private int pickNextScuteDropTime() {
      return this.random.nextInt(20 * TimeUtil.SECONDS_PER_MINUTE * 5) + 20 * TimeUtil.SECONDS_PER_MINUTE * 5;
   }

   @Override
   public void tick() {
      super.tick();
      if (this.level().isClientSide()) {
         this.setupAnimationStates();
      }

      if (this.isScared()) {
         this.clampHeadRotationToBody();
      }

      this.inStateTicks++;
   }

   @Override
   public float getAgeScale() {
      return this.isBaby() ? 0.6F : 1.0F;
   }

   private void setupAnimationStates() {
      switch (this.getState()) {
         case IDLE:
            this.rollOutAnimationState.stop();
            this.rollUpAnimationState.stop();
            this.peekAnimationState.stop();
            break;
         case ROLLING:
            this.rollOutAnimationState.stop();
            this.rollUpAnimationState.startIfStopped(this.tickCount);
            this.peekAnimationState.stop();
            break;
         case SCARED:
            this.rollOutAnimationState.stop();
            this.rollUpAnimationState.stop();
            if (this.peekReceivedClient) {
               this.peekAnimationState.stop();
               this.peekReceivedClient = false;
            }

            if (this.inStateTicks == 0L) {
               this.peekAnimationState.start(this.tickCount);
               this.peekAnimationState.fastForward(Armadillo.ArmadilloState.SCARED.animationDuration(), 1.0F);
            } else {
               this.peekAnimationState.startIfStopped(this.tickCount);
            }
            break;
         case UNROLLING:
            this.rollOutAnimationState.startIfStopped(this.tickCount);
            this.rollUpAnimationState.stop();
            this.peekAnimationState.stop();
      }
   }

   @Override
   public void handleEntityEvent(byte p_330641_) {
      if (p_330641_ == 64 && this.level().isClientSide()) {
         this.peekReceivedClient = true;
         this.level().playLocalSound(this.getX(), this.getY(), this.getZ(), SoundEvents.ARMADILLO_PEEK, this.getSoundSource(), 1.0F, 1.0F, false);
      } else {
         super.handleEntityEvent(p_330641_);
      }
   }

   @Override
   public boolean isFood(ItemStack p_333396_) {
      return p_333396_.is(ItemTags.ARMADILLO_FOOD);
   }

   public static boolean checkArmadilloSpawnRules(
      EntityType<Armadillo> p_328712_, LevelAccessor p_330410_, EntitySpawnReason p_367817_, BlockPos p_328785_, RandomSource p_328859_
   ) {
      return p_330410_.getBlockState(p_328785_.below()).is(BlockTags.ARMADILLO_SPAWNABLE_ON) && isBrightEnoughToSpawn(p_330410_, p_328785_);
   }

   public boolean isScaredBy(LivingEntity p_331619_) {
      if (!this.getBoundingBox().inflate(7.0, 2.0, 7.0).intersects(p_331619_.getBoundingBox())) {
         return false;
      } else if (p_331619_.getType().is(EntityTypeTags.UNDEAD)) {
         return true;
      } else if (this.getLastHurtByMob() == p_331619_) {
         return true;
      } else if (!(p_331619_ instanceof Player player)) {
         return false;
      } else {
         return player.isSpectator() ? false : player.isSprinting() || player.isPassenger();
      }
   }

   @Override
   protected void addAdditionalSaveData(ValueOutput p_406994_) {
      super.addAdditionalSaveData(p_406994_);
      p_406994_.store("state", Armadillo.ArmadilloState.CODEC, this.getState());
      p_406994_.putInt("scute_time", this.scuteTime);
   }

   @Override
   protected void readAdditionalSaveData(ValueInput p_408658_) {
      super.readAdditionalSaveData(p_408658_);
      this.switchToState(p_408658_.<Armadillo.ArmadilloState>read("state", Armadillo.ArmadilloState.CODEC).orElse(Armadillo.ArmadilloState.IDLE));
      p_408658_.getInt("scute_time").ifPresent(p_394978_ -> this.scuteTime = p_394978_);
   }

   public void rollUp() {
      if (!this.isScared()) {
         this.stopInPlace();
         this.resetLove();
         this.gameEvent(GameEvent.ENTITY_ACTION);
         this.makeSound(SoundEvents.ARMADILLO_ROLL);
         this.switchToState(Armadillo.ArmadilloState.ROLLING);
      }
   }

   public void rollOut() {
      if (this.isScared()) {
         this.gameEvent(GameEvent.ENTITY_ACTION);
         this.makeSound(SoundEvents.ARMADILLO_UNROLL_FINISH);
         this.switchToState(Armadillo.ArmadilloState.IDLE);
      }
   }

   @Override
   public boolean hurtServer(ServerLevel p_363151_, DamageSource p_369647_, float p_364733_) {
      if (this.isScared()) {
         p_364733_ = (p_364733_ - 1.0F) / 2.0F;
      }

      return super.hurtServer(p_363151_, p_369647_, p_364733_);
   }

   @Override
   protected void actuallyHurt(ServerLevel p_363157_, DamageSource p_328552_, float p_332199_) {
      super.actuallyHurt(p_363157_, p_328552_, p_332199_);
      if (!this.isNoAi() && !this.isDeadOrDying()) {
         if (p_328552_.getEntity() instanceof LivingEntity) {
            this.getBrain().setMemoryWithExpiry(MemoryModuleType.DANGER_DETECTED_RECENTLY, true, 80L);
            if (this.canStayRolledUp()) {
               this.rollUp();
            }
         } else if (p_328552_.is(DamageTypeTags.PANIC_ENVIRONMENTAL_CAUSES)) {
            this.rollOut();
         }
      }
   }

   @Override
   public InteractionResult mobInteract(Player p_335255_, InteractionHand p_331602_) {
      ItemStack itemstack = p_335255_.getItemInHand(p_331602_);
      if (itemstack.is(Items.BRUSH) && this.brushOffScute(p_335255_, itemstack)) {
         itemstack.hurtAndBreak(16, p_335255_, p_331602_.asEquipmentSlot());
         return InteractionResult.SUCCESS;
      } else {
         return this.isScared() ? InteractionResult.FAIL : super.mobInteract(p_335255_, p_331602_);
      }
   }

   public boolean brushOffScute(@Nullable Entity p_427976_, ItemStack p_427826_) {
      if (this.isBaby()) {
         return false;
      }

      if (this.level() instanceof ServerLevel serverlevel) {
         this.dropFromEntityInteractLootTable(serverlevel, BuiltInLootTables.ARMADILLO_BRUSH, p_427976_, p_427826_, this::spawnAtLocation);
         this.playSound(SoundEvents.ARMADILLO_BRUSH);
         this.gameEvent(GameEvent.ENTITY_INTERACT);
      }

      return true;
   }

   public boolean canStayRolledUp() {
      return !this.isPanicking() && !this.isInLiquid() && !this.isLeashed() && !this.isPassenger() && !this.isVehicle();
   }

   @Override
   public boolean canFallInLove() {
      return super.canFallInLove() && !this.isScared();
   }

   @Override
   protected SoundEvent getAmbientSound() {
      return this.isScared() ? null : SoundEvents.ARMADILLO_AMBIENT;
   }

   @Override
   protected void playEatingSound() {
      this.makeSound(SoundEvents.ARMADILLO_EAT);
   }

   @Override
   protected SoundEvent getDeathSound() {
      return SoundEvents.ARMADILLO_DEATH;
   }

   @Override
   protected SoundEvent getHurtSound(DamageSource p_335086_) {
      return this.isScared() ? SoundEvents.ARMADILLO_HURT_REDUCED : SoundEvents.ARMADILLO_HURT;
   }

   @Override
   protected void playStepSound(BlockPos p_333806_, BlockState p_333410_) {
      this.playSound(SoundEvents.ARMADILLO_STEP, 0.15F, 1.0F);
   }

   @Override
   public int getMaxHeadYRot() {
      return this.isScared() ? 0 : 32;
   }

   @Override
   protected BodyRotationControl createBodyControl() {
      return new BodyRotationControl(this) {
         @Override
         public void clientTick() {
            if (!Armadillo.this.isScared()) {
               super.clientTick();
            }
         }
      };
   }

   public enum ArmadilloState implements StringRepresentable {
      IDLE("idle", false, 0, 0) {
         @Override
         public boolean shouldHideInShell(long p_328572_) {
            return false;
         }
      },
      ROLLING("rolling", true, 10, 1) {
         @Override
         public boolean shouldHideInShell(long p_328385_) {
            return p_328385_ > 5L;
         }
      },
      SCARED("scared", true, 50, 2) {
         @Override
         public boolean shouldHideInShell(long p_327676_) {
            return true;
         }
      },
      UNROLLING("unrolling", true, 30, 3) {
         @Override
         public boolean shouldHideInShell(long p_328518_) {
            return p_328518_ < 26L;
         }
      };

      static final Codec<Armadillo.ArmadilloState> CODEC = StringRepresentable.fromEnum(Armadillo.ArmadilloState::values);
      private static final IntFunction<Armadillo.ArmadilloState> BY_ID = ByIdMap.continuous(
         Armadillo.ArmadilloState::id, values(), ByIdMap.OutOfBoundsStrategy.ZERO
      );
      public static final StreamCodec<ByteBuf, Armadillo.ArmadilloState> STREAM_CODEC = ByteBufCodecs.idMapper(BY_ID, Armadillo.ArmadilloState::id);
      private final String name;
      private final boolean isThreatened;
      private final int animationDuration;
      private final int id;

      ArmadilloState(final String p_327882_, final boolean p_330882_, final int p_328662_, final int p_328018_) {
         this.name = p_327882_;
         this.isThreatened = p_330882_;
         this.animationDuration = p_328662_;
         this.id = p_328018_;
      }

      @Override
      public String getSerializedName() {
         return this.name;
      }

      private int id() {
         return this.id;
      }

      public abstract boolean shouldHideInShell(long var1);

      public boolean isThreatened() {
         return this.isThreatened;
      }

      public int animationDuration() {
         return this.animationDuration;
      }
   }
}
