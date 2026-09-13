package net.minecraft.world.entity.animal.rabbit;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import java.util.function.IntFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.util.Util;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.JumpControl;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.ClimbOnTopOfPowderSnowGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.MoveToBlockGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CarrotBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class Rabbit extends Animal {
   public static final double STROLL_SPEED_MOD = 0.6;
   public static final double BREED_SPEED_MOD = 0.8;
   public static final double FOLLOW_SPEED_MOD = 1.0;
   public static final double FLEE_SPEED_MOD = 2.2;
   public static final double ATTACK_SPEED_MOD = 1.4;
   private static final EntityDataAccessor<Integer> DATA_TYPE_ID = SynchedEntityData.defineId(Rabbit.class, EntityDataSerializers.INT);
   private static final int DEFAULT_MORE_CARROT_TICKS = 0;
   private static final Identifier KILLER_BUNNY = Identifier.withDefaultNamespace("killer_bunny");
   private static final int DEFAULT_ATTACK_POWER = 3;
   private static final int EVIL_ATTACK_POWER_INCREMENT = 5;
   private static final Identifier EVIL_ATTACK_POWER_MODIFIER = Identifier.withDefaultNamespace("evil");
   private static final int EVIL_ARMOR_VALUE = 8;
   private static final int MORE_CARROTS_DELAY = 40;
   private int jumpTicks;
   private int jumpDuration;
   private boolean wasOnGround;
   private int jumpDelayTicks;
   int moreCarrotTicks = 0;

   public Rabbit(EntityType<? extends Rabbit> p_450574_, Level p_455846_) {
      super(p_450574_, p_455846_);
      this.jumpControl = new Rabbit.RabbitJumpControl(this);
      this.moveControl = new Rabbit.RabbitMoveControl(this);
      this.setSpeedModifier(0.0);
   }

   @Override
   protected void registerGoals() {
      this.goalSelector.addGoal(1, new FloatGoal(this));
      this.goalSelector.addGoal(1, new ClimbOnTopOfPowderSnowGoal(this, this.level()));
      this.goalSelector.addGoal(1, new Rabbit.RabbitPanicGoal(this, 2.2));
      this.goalSelector.addGoal(2, new BreedGoal(this, 0.8));
      this.goalSelector.addGoal(3, new TemptGoal(this, 1.0, p_451901_ -> p_451901_.is(ItemTags.RABBIT_FOOD), false));
      this.goalSelector.addGoal(4, new Rabbit.RabbitAvoidEntityGoal<>(this, Player.class, 8.0F, 2.2, 2.2));
      this.goalSelector.addGoal(4, new Rabbit.RabbitAvoidEntityGoal<>(this, Wolf.class, 10.0F, 2.2, 2.2));
      this.goalSelector.addGoal(4, new Rabbit.RabbitAvoidEntityGoal<>(this, Monster.class, 4.0F, 2.2, 2.2));
      this.goalSelector.addGoal(5, new Rabbit.RaidGardenGoal(this));
      this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.6));
      this.goalSelector.addGoal(11, new LookAtPlayerGoal(this, Player.class, 10.0F));
   }

   @Override
   protected float getJumpPower() {
      float f = 0.3F;
      if (this.moveControl.getSpeedModifier() <= 0.6) {
         f = 0.2F;
      }

      Path path = this.navigation.getPath();
      if (path != null && !path.isDone()) {
         Vec3 vec3 = path.getNextEntityPos(this);
         if (vec3.y > this.getY() + 0.5) {
            f = 0.5F;
         }
      }

      if (this.horizontalCollision || this.jumping && this.moveControl.getWantedY() > this.getY() + 0.5) {
         f = 0.5F;
      }

      return super.getJumpPower(f / 0.42F);
   }

   @Override
   public void jumpFromGround() {
      super.jumpFromGround();
      double d0 = this.moveControl.getSpeedModifier();
      if (d0 > 0.0) {
         double d1 = this.getDeltaMovement().horizontalDistanceSqr();
         if (d1 < 0.01) {
            this.moveRelative(0.1F, new Vec3(0.0, 0.0, 1.0));
         }
      }

      if (!this.level().isClientSide()) {
         this.level().broadcastEntityEvent(this, (byte)1);
      }
   }

   public float getJumpCompletion(float p_452015_) {
      return this.jumpDuration == 0 ? 0.0F : (this.jumpTicks + p_452015_) / this.jumpDuration;
   }

   public void setSpeedModifier(double p_452949_) {
      this.getNavigation().setSpeedModifier(p_452949_);
      this.moveControl.setWantedPosition(this.moveControl.getWantedX(), this.moveControl.getWantedY(), this.moveControl.getWantedZ(), p_452949_);
   }

   @Override
   public void setJumping(boolean p_457825_) {
      super.setJumping(p_457825_);
      if (p_457825_) {
         this.playSound(this.getJumpSound(), this.getSoundVolume(), ((this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.0F) * 0.8F);
      }
   }

   public void startJumping() {
      this.setJumping(true);
      this.jumpDuration = 10;
      this.jumpTicks = 0;
   }

   @Override
   protected void defineSynchedData(SynchedEntityData.Builder p_453884_) {
      super.defineSynchedData(p_453884_);
      p_453884_.define(DATA_TYPE_ID, Rabbit.Variant.DEFAULT.id);
   }

   @Override
   public void customServerAiStep(ServerLevel p_458398_) {
      if (this.jumpDelayTicks > 0) {
         this.jumpDelayTicks--;
      }

      if (this.moreCarrotTicks > 0) {
         this.moreCarrotTicks = this.moreCarrotTicks - this.random.nextInt(3);
         if (this.moreCarrotTicks < 0) {
            this.moreCarrotTicks = 0;
         }
      }

      if (this.onGround()) {
         if (!this.wasOnGround) {
            this.setJumping(false);
            this.checkLandingDelay();
         }

         if (this.getVariant() == Rabbit.Variant.EVIL && this.jumpDelayTicks == 0) {
            LivingEntity livingentity = this.getTarget();
            if (livingentity != null && this.distanceToSqr(livingentity) < 16.0) {
               this.facePoint(livingentity.getX(), livingentity.getZ());
               this.moveControl.setWantedPosition(livingentity.getX(), livingentity.getY(), livingentity.getZ(), this.moveControl.getSpeedModifier());
               this.startJumping();
               this.wasOnGround = true;
            }
         }

         Rabbit.RabbitJumpControl rabbit$rabbitjumpcontrol = (Rabbit.RabbitJumpControl)this.jumpControl;
         if (!rabbit$rabbitjumpcontrol.wantJump()) {
            if (this.moveControl.hasWanted() && this.jumpDelayTicks == 0) {
               Path path = this.navigation.getPath();
               Vec3 vec3 = new Vec3(this.moveControl.getWantedX(), this.moveControl.getWantedY(), this.moveControl.getWantedZ());
               if (path != null && !path.isDone()) {
                  vec3 = path.getNextEntityPos(this);
               }

               this.facePoint(vec3.x, vec3.z);
               this.startJumping();
            }
         } else if (!rabbit$rabbitjumpcontrol.canJump()) {
            this.enableJumpControl();
         }
      }

      this.wasOnGround = this.onGround();
   }

   @Override
   public boolean canSpawnSprintParticle() {
      return false;
   }

   private void facePoint(double p_450385_, double p_450675_) {
      this.setYRot((float)(Mth.atan2(p_450675_ - this.getZ(), p_450385_ - this.getX()) * 180.0 / (float) Math.PI) - 90.0F);
   }

   private void enableJumpControl() {
      ((Rabbit.RabbitJumpControl)this.jumpControl).setCanJump(true);
   }

   private void disableJumpControl() {
      ((Rabbit.RabbitJumpControl)this.jumpControl).setCanJump(false);
   }

   private void setLandingDelay() {
      if (this.moveControl.getSpeedModifier() < 2.2) {
         this.jumpDelayTicks = 10;
      } else {
         this.jumpDelayTicks = 1;
      }
   }

   private void checkLandingDelay() {
      this.setLandingDelay();
      this.disableJumpControl();
   }

   @Override
   public void aiStep() {
      super.aiStep();
      if (this.jumpTicks != this.jumpDuration) {
         this.jumpTicks++;
      } else if (this.jumpDuration != 0) {
         this.jumpTicks = 0;
         this.jumpDuration = 0;
         this.setJumping(false);
      }
   }

   public static AttributeSupplier.Builder createAttributes() {
      return Animal.createAnimalAttributes().add(Attributes.MAX_HEALTH, 3.0).add(Attributes.MOVEMENT_SPEED, 0.3F).add(Attributes.ATTACK_DAMAGE, 3.0);
   }

   @Override
   protected void addAdditionalSaveData(ValueOutput p_452895_) {
      super.addAdditionalSaveData(p_452895_);
      p_452895_.store("RabbitType", Rabbit.Variant.LEGACY_CODEC, this.getVariant());
      p_452895_.putInt("MoreCarrotTicks", this.moreCarrotTicks);
   }

   @Override
   protected void readAdditionalSaveData(ValueInput p_454498_) {
      super.readAdditionalSaveData(p_454498_);
      this.setVariant(p_454498_.<Rabbit.Variant>read("RabbitType", Rabbit.Variant.LEGACY_CODEC).orElse(Rabbit.Variant.DEFAULT));
      this.moreCarrotTicks = p_454498_.getIntOr("MoreCarrotTicks", 0);
   }

   protected SoundEvent getJumpSound() {
      return SoundEvents.RABBIT_JUMP;
   }

   @Override
   protected SoundEvent getAmbientSound() {
      return SoundEvents.RABBIT_AMBIENT;
   }

   @Override
   protected SoundEvent getHurtSound(DamageSource p_454528_) {
      return SoundEvents.RABBIT_HURT;
   }

   @Override
   protected SoundEvent getDeathSound() {
      return SoundEvents.RABBIT_DEATH;
   }

   @Override
   public void playAttackSound() {
      if (this.getVariant() == Rabbit.Variant.EVIL) {
         this.playSound(SoundEvents.RABBIT_ATTACK, 1.0F, (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.0F);
      }
   }

   @Override
   public SoundSource getSoundSource() {
      return this.getVariant() == Rabbit.Variant.EVIL ? SoundSource.HOSTILE : SoundSource.NEUTRAL;
   }

   public @Nullable Rabbit getBreedOffspring(ServerLevel p_456078_, AgeableMob p_454628_) {
      Rabbit rabbit = EntityType.RABBIT.create(p_456078_, EntitySpawnReason.BREEDING);
      if (rabbit != null) {
         Rabbit.Variant rabbit$variant = getRandomRabbitVariant(p_456078_, this.blockPosition());
         if (this.random.nextInt(20) != 0) {
            if (p_454628_ instanceof Rabbit rabbit1 && this.random.nextBoolean()) {
               rabbit$variant = rabbit1.getVariant();
            } else {
               rabbit$variant = this.getVariant();
            }
         }

         rabbit.setVariant(rabbit$variant);
      }

      return rabbit;
   }

   @Override
   public boolean isFood(ItemStack p_455645_) {
      return p_455645_.is(ItemTags.RABBIT_FOOD);
   }

   public Rabbit.Variant getVariant() {
      return Rabbit.Variant.byId(this.entityData.get(DATA_TYPE_ID));
   }

   private void setVariant(Rabbit.Variant p_457495_) {
      if (p_457495_ == Rabbit.Variant.EVIL) {
         this.getAttribute(Attributes.ARMOR).setBaseValue(8.0);
         this.goalSelector.addGoal(4, new MeleeAttackGoal(this, 1.4, true));
         this.targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
         this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
         this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Wolf.class, true));
         this.getAttribute(Attributes.ATTACK_DAMAGE)
            .addOrUpdateTransientModifier(new AttributeModifier(EVIL_ATTACK_POWER_MODIFIER, 5.0, AttributeModifier.Operation.ADD_VALUE));
         if (!this.hasCustomName()) {
            this.setCustomName(Component.translatable(Util.makeDescriptionId("entity", KILLER_BUNNY)));
         }
      } else {
         this.getAttribute(Attributes.ATTACK_DAMAGE).removeModifier(EVIL_ATTACK_POWER_MODIFIER);
      }

      this.entityData.set(DATA_TYPE_ID, p_457495_.id);
   }

   @Override
   public <T> @Nullable T get(DataComponentType<? extends T> p_452307_) {
      return p_452307_ == DataComponents.RABBIT_VARIANT ? castComponentValue((DataComponentType<T>)p_452307_, this.getVariant()) : super.get(p_452307_);
   }

   @Override
   protected void applyImplicitComponents(DataComponentGetter p_450910_) {
      this.applyImplicitComponentIfPresent(p_450910_, DataComponents.RABBIT_VARIANT);
      super.applyImplicitComponents(p_450910_);
   }

   @Override
   protected <T> boolean applyImplicitComponent(DataComponentType<T> p_450927_, T p_452168_) {
      if (p_450927_ == DataComponents.RABBIT_VARIANT) {
         this.setVariant(castComponentValue(DataComponents.RABBIT_VARIANT, p_452168_));
         return true;
      } else {
         return super.applyImplicitComponent(p_450927_, p_452168_);
      }
   }

   @Override
   public @Nullable SpawnGroupData finalizeSpawn(
      ServerLevelAccessor p_452013_, DifficultyInstance p_451735_, EntitySpawnReason p_460736_, @Nullable SpawnGroupData p_457011_
   ) {
      Rabbit.Variant rabbit$variant = getRandomRabbitVariant(p_452013_, this.blockPosition());
      if (p_457011_ instanceof Rabbit.RabbitGroupData) {
         rabbit$variant = ((Rabbit.RabbitGroupData)p_457011_).variant;
      } else {
         p_457011_ = new Rabbit.RabbitGroupData(rabbit$variant);
      }

      this.setVariant(rabbit$variant);
      return super.finalizeSpawn(p_452013_, p_451735_, p_460736_, p_457011_);
   }

   private static Rabbit.Variant getRandomRabbitVariant(LevelAccessor p_456845_, BlockPos p_454058_) {
      Holder<Biome> holder = p_456845_.getBiome(p_454058_);
      int i = p_456845_.getRandom().nextInt(100);
      if (holder.is(BiomeTags.SPAWNS_WHITE_RABBITS)) {
         return i < 80 ? Rabbit.Variant.WHITE : Rabbit.Variant.WHITE_SPLOTCHED;
      } else if (holder.is(BiomeTags.SPAWNS_GOLD_RABBITS)) {
         return Rabbit.Variant.GOLD;
      } else {
         return i < 50 ? Rabbit.Variant.BROWN : (i < 90 ? Rabbit.Variant.SALT : Rabbit.Variant.BLACK);
      }
   }

   public static boolean checkRabbitSpawnRules(
      EntityType<Rabbit> p_452708_, LevelAccessor p_459315_, EntitySpawnReason p_450493_, BlockPos p_457197_, RandomSource p_460486_
   ) {
      return p_459315_.getBlockState(p_457197_.below()).is(BlockTags.RABBITS_SPAWNABLE_ON) && isBrightEnoughToSpawn(p_459315_, p_457197_);
   }

   boolean wantsMoreFood() {
      return this.moreCarrotTicks <= 0;
   }

   @Override
   public void handleEntityEvent(byte p_454954_) {
      if (p_454954_ == 1) {
         this.spawnSprintParticle();
         this.jumpDuration = 10;
         this.jumpTicks = 0;
      } else {
         super.handleEntityEvent(p_454954_);
      }
   }

   @Override
   public Vec3 getLeashOffset() {
      return new Vec3(0.0, 0.6F * this.getEyeHeight(), this.getBbWidth() * 0.4F);
   }

   static class RabbitAvoidEntityGoal<T extends LivingEntity> extends AvoidEntityGoal<T> {
      private final Rabbit rabbit;

      public RabbitAvoidEntityGoal(Rabbit p_455438_, Class<T> p_458891_, float p_455712_, double p_452935_, double p_452953_) {
         super(p_455438_, p_458891_, p_455712_, p_452935_, p_452953_);
         this.rabbit = p_455438_;
      }

      @Override
      public boolean canUse() {
         return this.rabbit.getVariant() != Rabbit.Variant.EVIL && super.canUse();
      }
   }

   public static class RabbitGroupData extends AgeableMob.AgeableMobGroupData {
      public final Rabbit.Variant variant;

      public RabbitGroupData(Rabbit.Variant p_456787_) {
         super(1.0F);
         this.variant = p_456787_;
      }
   }

   public static class RabbitJumpControl extends JumpControl {
      private final Rabbit rabbit;
      private boolean canJump;

      public RabbitJumpControl(Rabbit p_454022_) {
         super(p_454022_);
         this.rabbit = p_454022_;
      }

      public boolean wantJump() {
         return this.jump;
      }

      public boolean canJump() {
         return this.canJump;
      }

      public void setCanJump(boolean p_452395_) {
         this.canJump = p_452395_;
      }

      @Override
      public void tick() {
         if (this.jump) {
            this.rabbit.startJumping();
            this.jump = false;
         }
      }
   }

   static class RabbitMoveControl extends MoveControl {
      private final Rabbit rabbit;
      private double nextJumpSpeed;

      public RabbitMoveControl(Rabbit p_453850_) {
         super(p_453850_);
         this.rabbit = p_453850_;
      }

      @Override
      public void tick() {
         if (this.rabbit.onGround() && !this.rabbit.jumping && !((Rabbit.RabbitJumpControl)this.rabbit.jumpControl).wantJump()) {
            this.rabbit.setSpeedModifier(0.0);
         } else if (this.hasWanted() || this.operation == MoveControl.Operation.JUMPING) {
            this.rabbit.setSpeedModifier(this.nextJumpSpeed);
         }

         super.tick();
      }

      @Override
      public void setWantedPosition(double p_460409_, double p_460799_, double p_455919_, double p_456448_) {
         if (this.rabbit.isInWater()) {
            p_456448_ = 1.5;
         }

         super.setWantedPosition(p_460409_, p_460799_, p_455919_, p_456448_);
         if (p_456448_ > 0.0) {
            this.nextJumpSpeed = p_456448_;
         }
      }
   }

   static class RabbitPanicGoal extends PanicGoal {
      private final Rabbit rabbit;

      public RabbitPanicGoal(Rabbit p_451709_, double p_452914_) {
         super(p_451709_, p_452914_);
         this.rabbit = p_451709_;
      }

      @Override
      public void tick() {
         super.tick();
         this.rabbit.setSpeedModifier(this.speedModifier);
      }
   }

   static class RaidGardenGoal extends MoveToBlockGoal {
      private final Rabbit rabbit;
      private boolean wantsToRaid;
      private boolean canRaid;

      public RaidGardenGoal(Rabbit p_454332_) {
         super(p_454332_, 0.7F, 16);
         this.rabbit = p_454332_;
      }

      @Override
      public boolean canUse() {
         if (this.nextStartTick <= 0) {
            if (!getServerLevel(this.rabbit).getGameRules().get(GameRules.MOB_GRIEFING)) {
               return false;
            }

            this.canRaid = false;
            this.wantsToRaid = this.rabbit.wantsMoreFood();
         }

         return super.canUse();
      }

      @Override
      public boolean canContinueToUse() {
         return this.canRaid && super.canContinueToUse();
      }

      @Override
      public void tick() {
         super.tick();
         this.rabbit
            .getLookControl()
            .setLookAt(this.blockPos.getX() + 0.5, this.blockPos.getY() + 1, this.blockPos.getZ() + 0.5, 10.0F, this.rabbit.getMaxHeadXRot());
         if (this.isReachedTarget()) {
            Level level = this.rabbit.level();
            BlockPos blockpos = this.blockPos.above();
            BlockState blockstate = level.getBlockState(blockpos);
            Block block = blockstate.getBlock();
            if (this.canRaid && block instanceof CarrotBlock) {
               int i = blockstate.getValue(CarrotBlock.AGE);
               if (i == 0) {
                  level.setBlock(blockpos, Blocks.AIR.defaultBlockState(), 2);
                  level.destroyBlock(blockpos, true, this.rabbit);
               } else {
                  level.setBlock(blockpos, blockstate.setValue(CarrotBlock.AGE, i - 1), 2);
                  level.gameEvent(GameEvent.BLOCK_CHANGE, blockpos, GameEvent.Context.of(this.rabbit));
                  level.levelEvent(2001, blockpos, Block.getId(blockstate));
               }

               this.rabbit.moreCarrotTicks = 40;
            }

            this.canRaid = false;
            this.nextStartTick = 10;
         }
      }

      @Override
      protected boolean isValidTarget(LevelReader p_457725_, BlockPos p_456006_) {
         BlockState blockstate = p_457725_.getBlockState(p_456006_);
         if (blockstate.is(Blocks.FARMLAND) && this.wantsToRaid && !this.canRaid) {
            blockstate = p_457725_.getBlockState(p_456006_.above());
            if (blockstate.getBlock() instanceof CarrotBlock && ((CarrotBlock)blockstate.getBlock()).isMaxAge(blockstate)) {
               this.canRaid = true;
               return true;
            }
         }

         return false;
      }
   }

   public enum Variant implements StringRepresentable {
      BROWN(0, "brown"),
      WHITE(1, "white"),
      BLACK(2, "black"),
      WHITE_SPLOTCHED(3, "white_splotched"),
      GOLD(4, "gold"),
      SALT(5, "salt"),
      EVIL(99, "evil");

      public static final Rabbit.Variant DEFAULT = BROWN;
      private static final IntFunction<Rabbit.Variant> BY_ID = ByIdMap.sparse(Rabbit.Variant::id, values(), DEFAULT);
      public static final Codec<Rabbit.Variant> CODEC = StringRepresentable.fromEnum(Rabbit.Variant::values);
      @Deprecated
      public static final Codec<Rabbit.Variant> LEGACY_CODEC = Codec.INT.xmap(BY_ID::apply, Rabbit.Variant::id);
      public static final StreamCodec<ByteBuf, Rabbit.Variant> STREAM_CODEC = ByteBufCodecs.idMapper(BY_ID, Rabbit.Variant::id);
      final int id;
      private final String name;

      Variant(final int p_455053_, final String p_454821_) {
         this.id = p_455053_;
         this.name = p_454821_;
      }

      @Override
      public String getSerializedName() {
         return this.name;
      }

      public int id() {
         return this.id;
      }

      public static Rabbit.Variant byId(int p_452941_) {
         return BY_ID.apply(p_452941_);
      }
   }
}
