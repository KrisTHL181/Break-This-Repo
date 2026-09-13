package net.minecraft.world.entity.animal.parrot;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import java.util.List;
import java.util.Map;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.util.Util;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowMobGoal;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.world.entity.ai.goal.LandOnOwnersShoulderGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.SitWhenOrderedToGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomFlyingGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.FlyingAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class Parrot extends ShoulderRidingEntity implements FlyingAnimal {
   private static final EntityDataAccessor<Integer> DATA_VARIANT_ID = SynchedEntityData.defineId(Parrot.class, EntityDataSerializers.INT);
   private static final Predicate<Mob> NOT_PARROT_PREDICATE = new Predicate<Mob>() {
      public boolean test(@Nullable Mob p_460699_) {
         return p_460699_ != null && Parrot.MOB_SOUND_MAP.containsKey(p_460699_.getType());
      }
   };
   static final Map<EntityType<?>, SoundEvent> MOB_SOUND_MAP = Util.make(Maps.newHashMap(), p_454351_ -> {
      p_454351_.put(EntityType.BLAZE, SoundEvents.PARROT_IMITATE_BLAZE);
      p_454351_.put(EntityType.BOGGED, SoundEvents.PARROT_IMITATE_BOGGED);
      p_454351_.put(EntityType.BREEZE, SoundEvents.PARROT_IMITATE_BREEZE);
      p_454351_.put(EntityType.CAMEL_HUSK, SoundEvents.PARROT_IMITATE_CAMEL_HUSK);
      p_454351_.put(EntityType.CAVE_SPIDER, SoundEvents.PARROT_IMITATE_SPIDER);
      p_454351_.put(EntityType.CREAKING, SoundEvents.PARROT_IMITATE_CREAKING);
      p_454351_.put(EntityType.CREEPER, SoundEvents.PARROT_IMITATE_CREEPER);
      p_454351_.put(EntityType.DROWNED, SoundEvents.PARROT_IMITATE_DROWNED);
      p_454351_.put(EntityType.ELDER_GUARDIAN, SoundEvents.PARROT_IMITATE_ELDER_GUARDIAN);
      p_454351_.put(EntityType.ENDER_DRAGON, SoundEvents.PARROT_IMITATE_ENDER_DRAGON);
      p_454351_.put(EntityType.ENDERMITE, SoundEvents.PARROT_IMITATE_ENDERMITE);
      p_454351_.put(EntityType.EVOKER, SoundEvents.PARROT_IMITATE_EVOKER);
      p_454351_.put(EntityType.GHAST, SoundEvents.PARROT_IMITATE_GHAST);
      p_454351_.put(EntityType.HAPPY_GHAST, SoundEvents.EMPTY);
      p_454351_.put(EntityType.GUARDIAN, SoundEvents.PARROT_IMITATE_GUARDIAN);
      p_454351_.put(EntityType.HOGLIN, SoundEvents.PARROT_IMITATE_HOGLIN);
      p_454351_.put(EntityType.HUSK, SoundEvents.PARROT_IMITATE_HUSK);
      p_454351_.put(EntityType.ILLUSIONER, SoundEvents.PARROT_IMITATE_ILLUSIONER);
      p_454351_.put(EntityType.MAGMA_CUBE, SoundEvents.PARROT_IMITATE_MAGMA_CUBE);
      p_454351_.put(EntityType.PARCHED, SoundEvents.PARROT_IMITATE_PARCHED);
      p_454351_.put(EntityType.PHANTOM, SoundEvents.PARROT_IMITATE_PHANTOM);
      p_454351_.put(EntityType.PIGLIN, SoundEvents.PARROT_IMITATE_PIGLIN);
      p_454351_.put(EntityType.PIGLIN_BRUTE, SoundEvents.PARROT_IMITATE_PIGLIN_BRUTE);
      p_454351_.put(EntityType.PILLAGER, SoundEvents.PARROT_IMITATE_PILLAGER);
      p_454351_.put(EntityType.RAVAGER, SoundEvents.PARROT_IMITATE_RAVAGER);
      p_454351_.put(EntityType.SHULKER, SoundEvents.PARROT_IMITATE_SHULKER);
      p_454351_.put(EntityType.SILVERFISH, SoundEvents.PARROT_IMITATE_SILVERFISH);
      p_454351_.put(EntityType.SKELETON, SoundEvents.PARROT_IMITATE_SKELETON);
      p_454351_.put(EntityType.SLIME, SoundEvents.PARROT_IMITATE_SLIME);
      p_454351_.put(EntityType.SPIDER, SoundEvents.PARROT_IMITATE_SPIDER);
      p_454351_.put(EntityType.STRAY, SoundEvents.PARROT_IMITATE_STRAY);
      p_454351_.put(EntityType.VEX, SoundEvents.PARROT_IMITATE_VEX);
      p_454351_.put(EntityType.VINDICATOR, SoundEvents.PARROT_IMITATE_VINDICATOR);
      p_454351_.put(EntityType.WARDEN, SoundEvents.PARROT_IMITATE_WARDEN);
      p_454351_.put(EntityType.WITCH, SoundEvents.PARROT_IMITATE_WITCH);
      p_454351_.put(EntityType.WITHER, SoundEvents.PARROT_IMITATE_WITHER);
      p_454351_.put(EntityType.WITHER_SKELETON, SoundEvents.PARROT_IMITATE_WITHER_SKELETON);
      p_454351_.put(EntityType.ZOGLIN, SoundEvents.PARROT_IMITATE_ZOGLIN);
      p_454351_.put(EntityType.ZOMBIE, SoundEvents.PARROT_IMITATE_ZOMBIE);
      p_454351_.put(EntityType.ZOMBIE_HORSE, SoundEvents.PARROT_IMITATE_ZOMBIE_HORSE);
      p_454351_.put(EntityType.ZOMBIE_NAUTILUS, SoundEvents.PARROT_IMITATE_ZOMBIE_NAUTILUS);
      p_454351_.put(EntityType.ZOMBIE_VILLAGER, SoundEvents.PARROT_IMITATE_ZOMBIE_VILLAGER);
   });
   public float flap;
   public float flapSpeed;
   public float oFlapSpeed;
   public float oFlap;
   private float flapping = 1.0F;
   private float nextFlap = 1.0F;
   private boolean partyParrot;
   private @Nullable BlockPos jukebox;

   public Parrot(EntityType<? extends Parrot> p_451378_, Level p_451488_) {
      super(p_451378_, p_451488_);
      this.moveControl = new FlyingMoveControl(this, 10, false);
      this.setPathfindingMalus(PathType.DANGER_FIRE, -1.0F);
      this.setPathfindingMalus(PathType.DAMAGE_FIRE, -1.0F);
      this.setPathfindingMalus(PathType.COCOA, -1.0F);
   }

   @Override
   public @Nullable SpawnGroupData finalizeSpawn(
      ServerLevelAccessor p_460554_, DifficultyInstance p_451085_, EntitySpawnReason p_459029_, @Nullable SpawnGroupData p_457071_
   ) {
      this.setVariant(Util.getRandom(Parrot.Variant.values(), p_460554_.getRandom()));
      if (p_457071_ == null) {
         p_457071_ = new AgeableMob.AgeableMobGroupData(false);
      }

      return super.finalizeSpawn(p_460554_, p_451085_, p_459029_, p_457071_);
   }

   @Override
   public boolean isBaby() {
      return false;
   }

   @Override
   protected void registerGoals() {
      this.goalSelector.addGoal(0, new TamableAnimal.TamableAnimalPanicGoal(1.25));
      this.goalSelector.addGoal(0, new FloatGoal(this));
      this.goalSelector.addGoal(1, new LookAtPlayerGoal(this, Player.class, 8.0F));
      this.goalSelector.addGoal(2, new SitWhenOrderedToGoal(this));
      this.goalSelector.addGoal(2, new FollowOwnerGoal(this, 1.0, 5.0F, 1.0F));
      this.goalSelector.addGoal(2, new Parrot.ParrotWanderGoal(this, 1.0));
      this.goalSelector.addGoal(3, new LandOnOwnersShoulderGoal(this));
      this.goalSelector.addGoal(3, new FollowMobGoal(this, 1.0, 3.0F, 7.0F));
   }

   public static AttributeSupplier.Builder createAttributes() {
      return Animal.createAnimalAttributes()
         .add(Attributes.MAX_HEALTH, 6.0)
         .add(Attributes.FLYING_SPEED, 0.4F)
         .add(Attributes.MOVEMENT_SPEED, 0.2F)
         .add(Attributes.ATTACK_DAMAGE, 3.0);
   }

   @Override
   protected PathNavigation createNavigation(Level p_460947_) {
      FlyingPathNavigation flyingpathnavigation = new FlyingPathNavigation(this, p_460947_);
      flyingpathnavigation.setCanOpenDoors(false);
      flyingpathnavigation.setCanFloat(true);
      return flyingpathnavigation;
   }

   @Override
   public void aiStep() {
      if (this.jukebox == null || !this.jukebox.closerToCenterThan(this.position(), 3.46) || !this.level().getBlockState(this.jukebox).is(Blocks.JUKEBOX)) {
         this.partyParrot = false;
         this.jukebox = null;
      }

      if (this.level().random.nextInt(400) == 0) {
         imitateNearbyMobs(this.level(), this);
      }

      super.aiStep();
      this.calculateFlapping();
   }

   @Override
   public void setRecordPlayingNearby(BlockPos p_459555_, boolean p_459763_) {
      this.jukebox = p_459555_;
      this.partyParrot = p_459763_;
   }

   public boolean isPartyParrot() {
      return this.partyParrot;
   }

   private void calculateFlapping() {
      this.oFlap = this.flap;
      this.oFlapSpeed = this.flapSpeed;
      this.flapSpeed = this.flapSpeed + (!this.onGround() && !this.isPassenger() ? 4 : -1) * 0.3F;
      this.flapSpeed = Mth.clamp(this.flapSpeed, 0.0F, 1.0F);
      if (!this.onGround() && this.flapping < 1.0F) {
         this.flapping = 1.0F;
      }

      this.flapping *= 0.9F;
      Vec3 vec3 = this.getDeltaMovement();
      if (!this.onGround() && vec3.y < 0.0) {
         this.setDeltaMovement(vec3.multiply(1.0, 0.6, 1.0));
      }

      this.flap = this.flap + this.flapping * 2.0F;
   }

   public static boolean imitateNearbyMobs(Level p_450853_, Entity p_457969_) {
      if (p_457969_.isAlive() && !p_457969_.isSilent() && p_450853_.random.nextInt(2) == 0) {
         List<Mob> list = p_450853_.getEntitiesOfClass(Mob.class, p_457969_.getBoundingBox().inflate(20.0), NOT_PARROT_PREDICATE);
         if (!list.isEmpty()) {
            Mob mob = list.get(p_450853_.random.nextInt(list.size()));
            if (!mob.isSilent()) {
               SoundEvent soundevent = getImitatedSound(mob.getType());
               p_450853_.playSound(
                  null, p_457969_.getX(), p_457969_.getY(), p_457969_.getZ(), soundevent, p_457969_.getSoundSource(), 0.7F, getPitch(p_450853_.random)
               );
               return true;
            }
         }

         return false;
      } else {
         return false;
      }
   }

   @Override
   public InteractionResult mobInteract(Player p_453025_, InteractionHand p_451080_) {
      ItemStack itemstack = p_453025_.getItemInHand(p_451080_);
      if (!this.isTame() && itemstack.is(ItemTags.PARROT_FOOD)) {
         this.usePlayerItem(p_453025_, p_451080_, itemstack);
         if (!this.isSilent()) {
            this.level()
               .playSound(
                  null,
                  this.getX(),
                  this.getY(),
                  this.getZ(),
                  SoundEvents.PARROT_EAT,
                  this.getSoundSource(),
                  1.0F,
                  1.0F + (this.random.nextFloat() - this.random.nextFloat()) * 0.2F
               );
         }

         if (!this.level().isClientSide()) {
            if (this.random.nextInt(10) == 0) {
               this.tame(p_453025_);
               this.level().broadcastEntityEvent(this, (byte)7);
            } else {
               this.level().broadcastEntityEvent(this, (byte)6);
            }
         }

         return InteractionResult.SUCCESS;
      } else if (!itemstack.is(ItemTags.PARROT_POISONOUS_FOOD)) {
         if (!this.isFlying() && this.isTame() && this.isOwnedBy(p_453025_)) {
            if (!this.level().isClientSide()) {
               this.setOrderedToSit(!this.isOrderedToSit());
            }

            return InteractionResult.SUCCESS;
         } else {
            return super.mobInteract(p_453025_, p_451080_);
         }
      } else {
         this.usePlayerItem(p_453025_, p_451080_, itemstack);
         this.addEffect(new MobEffectInstance(MobEffects.POISON, 900));
         if (p_453025_.isCreative() || !this.isInvulnerable()) {
            this.hurt(this.damageSources().playerAttack(p_453025_), Float.MAX_VALUE);
         }

         return InteractionResult.SUCCESS;
      }
   }

   @Override
   public boolean isFood(ItemStack p_456951_) {
      return false;
   }

   public static boolean checkParrotSpawnRules(
      EntityType<Parrot> p_450702_, LevelAccessor p_458978_, EntitySpawnReason p_456137_, BlockPos p_460604_, RandomSource p_454376_
   ) {
      return p_458978_.getBlockState(p_460604_.below()).is(BlockTags.PARROTS_SPAWNABLE_ON) && isBrightEnoughToSpawn(p_458978_, p_460604_);
   }

   @Override
   protected void checkFallDamage(double p_452139_, boolean p_450373_, BlockState p_451743_, BlockPos p_455918_) {
   }

   @Override
   public boolean canMate(Animal p_454134_) {
      return false;
   }

   @Override
   public @Nullable AgeableMob getBreedOffspring(ServerLevel p_456081_, AgeableMob p_454929_) {
      return null;
   }

   @Override
   public @Nullable SoundEvent getAmbientSound() {
      return getAmbient(this.level(), this.level().random);
   }

   public static SoundEvent getAmbient(Level p_455368_, RandomSource p_454414_) {
      if (p_455368_.getDifficulty() != Difficulty.PEACEFUL && p_454414_.nextInt(1000) == 0) {
         List<EntityType<?>> list = Lists.newArrayList(MOB_SOUND_MAP.keySet());
         return getImitatedSound(list.get(p_454414_.nextInt(list.size())));
      } else {
         return SoundEvents.PARROT_AMBIENT;
      }
   }

   private static SoundEvent getImitatedSound(EntityType<?> p_455092_) {
      return MOB_SOUND_MAP.getOrDefault(p_455092_, SoundEvents.PARROT_AMBIENT);
   }

   @Override
   protected SoundEvent getHurtSound(DamageSource p_460052_) {
      return SoundEvents.PARROT_HURT;
   }

   @Override
   protected SoundEvent getDeathSound() {
      return SoundEvents.PARROT_DEATH;
   }

   @Override
   protected void playStepSound(BlockPos p_458329_, BlockState p_453186_) {
      this.playSound(SoundEvents.PARROT_STEP, 0.15F, 1.0F);
   }

   @Override
   protected boolean isFlapping() {
      return this.flyDist > this.nextFlap;
   }

   @Override
   protected void onFlap() {
      this.playSound(SoundEvents.PARROT_FLY, 0.15F, 1.0F);
      this.nextFlap = this.flyDist + this.flapSpeed / 2.0F;
   }

   @Override
   public float getVoicePitch() {
      return getPitch(this.random);
   }

   public static float getPitch(RandomSource p_457503_) {
      return (p_457503_.nextFloat() - p_457503_.nextFloat()) * 0.2F + 1.0F;
   }

   @Override
   public SoundSource getSoundSource() {
      return SoundSource.NEUTRAL;
   }

   @Override
   public boolean isPushable() {
      return true;
   }

   @Override
   protected void doPush(Entity p_457543_) {
      if (!(p_457543_ instanceof Player)) {
         super.doPush(p_457543_);
      }
   }

   @Override
   public boolean hurtServer(ServerLevel p_451725_, DamageSource p_451934_, float p_458835_) {
      if (this.isInvulnerableTo(p_451725_, p_451934_)) {
         return false;
      }

      this.setOrderedToSit(false);
      return super.hurtServer(p_451725_, p_451934_, p_458835_);
   }

   public Parrot.Variant getVariant() {
      return Parrot.Variant.byId(this.entityData.get(DATA_VARIANT_ID));
   }

   private void setVariant(Parrot.Variant p_453188_) {
      this.entityData.set(DATA_VARIANT_ID, p_453188_.id);
   }

   @Override
   public <T> @Nullable T get(DataComponentType<? extends T> p_458479_) {
      return p_458479_ == DataComponents.PARROT_VARIANT ? castComponentValue((DataComponentType<T>)p_458479_, this.getVariant()) : super.get(p_458479_);
   }

   @Override
   protected void applyImplicitComponents(DataComponentGetter p_450233_) {
      this.applyImplicitComponentIfPresent(p_450233_, DataComponents.PARROT_VARIANT);
      super.applyImplicitComponents(p_450233_);
   }

   @Override
   protected <T> boolean applyImplicitComponent(DataComponentType<T> p_451573_, T p_452525_) {
      if (p_451573_ == DataComponents.PARROT_VARIANT) {
         this.setVariant(castComponentValue(DataComponents.PARROT_VARIANT, p_452525_));
         return true;
      } else {
         return super.applyImplicitComponent(p_451573_, p_452525_);
      }
   }

   @Override
   protected void defineSynchedData(SynchedEntityData.Builder p_450905_) {
      super.defineSynchedData(p_450905_);
      p_450905_.define(DATA_VARIANT_ID, Parrot.Variant.DEFAULT.id);
   }

   @Override
   protected void addAdditionalSaveData(ValueOutput p_458730_) {
      super.addAdditionalSaveData(p_458730_);
      p_458730_.store("Variant", Parrot.Variant.LEGACY_CODEC, this.getVariant());
   }

   @Override
   protected void readAdditionalSaveData(ValueInput p_458363_) {
      super.readAdditionalSaveData(p_458363_);
      this.setVariant(p_458363_.<Parrot.Variant>read("Variant", Parrot.Variant.LEGACY_CODEC).orElse(Parrot.Variant.DEFAULT));
   }

   @Override
   public boolean isFlying() {
      return !this.onGround();
   }

   @Override
   protected boolean canFlyToOwner() {
      return true;
   }

   @Override
   public Vec3 getLeashOffset() {
      return new Vec3(0.0, 0.5F * this.getEyeHeight(), this.getBbWidth() * 0.4F);
   }

   static class ParrotWanderGoal extends WaterAvoidingRandomFlyingGoal {
      public ParrotWanderGoal(PathfinderMob p_455655_, double p_456832_) {
         super(p_455655_, p_456832_);
      }

      @Override
      protected @Nullable Vec3 getPosition() {
         Vec3 vec3 = null;
         if (this.mob.isInWater()) {
            vec3 = LandRandomPos.getPos(this.mob, 15, 15);
         }

         if (this.mob.getRandom().nextFloat() >= this.probability) {
            vec3 = this.getTreePos();
         }

         return vec3 == null ? super.getPosition() : vec3;
      }

      private @Nullable Vec3 getTreePos() {
         BlockPos blockpos = this.mob.blockPosition();
         BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();
         BlockPos.MutableBlockPos blockpos$mutableblockpos1 = new BlockPos.MutableBlockPos();

         for (BlockPos blockpos1 : BlockPos.betweenClosed(
            Mth.floor(this.mob.getX() - 3.0),
            Mth.floor(this.mob.getY() - 6.0),
            Mth.floor(this.mob.getZ() - 3.0),
            Mth.floor(this.mob.getX() + 3.0),
            Mth.floor(this.mob.getY() + 6.0),
            Mth.floor(this.mob.getZ() + 3.0)
         )) {
            if (!blockpos.equals(blockpos1)) {
               BlockState blockstate = this.mob.level().getBlockState(blockpos$mutableblockpos1.setWithOffset(blockpos1, Direction.DOWN));
               boolean flag = blockstate.getBlock() instanceof LeavesBlock || blockstate.is(BlockTags.LOGS);
               if (flag
                  && this.mob.level().isEmptyBlock(blockpos1)
                  && this.mob.level().isEmptyBlock(blockpos$mutableblockpos.setWithOffset(blockpos1, Direction.UP))) {
                  return Vec3.atBottomCenterOf(blockpos1);
               }
            }
         }

         return null;
      }
   }

   public enum Variant implements StringRepresentable {
      RED_BLUE(0, "red_blue"),
      BLUE(1, "blue"),
      GREEN(2, "green"),
      YELLOW_BLUE(3, "yellow_blue"),
      GRAY(4, "gray");

      public static final Parrot.Variant DEFAULT = RED_BLUE;
      private static final IntFunction<Parrot.Variant> BY_ID = ByIdMap.continuous(Parrot.Variant::getId, values(), ByIdMap.OutOfBoundsStrategy.CLAMP);
      public static final Codec<Parrot.Variant> CODEC = StringRepresentable.fromEnum(Parrot.Variant::values);
      @Deprecated
      public static final Codec<Parrot.Variant> LEGACY_CODEC = Codec.INT.xmap(BY_ID::apply, Parrot.Variant::getId);
      public static final StreamCodec<ByteBuf, Parrot.Variant> STREAM_CODEC = ByteBufCodecs.idMapper(BY_ID, Parrot.Variant::getId);
      final int id;
      private final String name;

      Variant(final int p_458289_, final String p_453415_) {
         this.id = p_458289_;
         this.name = p_453415_;
      }

      public int getId() {
         return this.id;
      }

      public static Parrot.Variant byId(int p_456311_) {
         return BY_ID.apply(p_456311_);
      }

      @Override
      public String getSerializedName() {
         return this.name;
      }
   }
}
