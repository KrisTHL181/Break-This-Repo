package net.minecraft.world.entity.animal;

import com.google.common.collect.UnmodifiableIterator;
import java.util.Optional;
import net.minecraft.advancements.triggers.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.DismountHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockAndLightGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public abstract class Animal extends AgeableMob {
   protected static final int PARENT_AGE_AFTER_BREEDING = 6000;
   private static final int DEFAULT_IN_LOVE_TIME = 0;
   private int inLove = 0;
   private @Nullable EntityReference<ServerPlayer> loveCause;

   protected Animal(final EntityType<? extends Animal> type, final Level level) {
      super(type, level);
      this.setPathfindingMalus(PathType.FIRE_IN_NEIGHBOR, 16.0F);
      this.setPathfindingMalus(PathType.FIRE, -1.0F);
   }

   public static AttributeSupplier.Builder createAnimalAttributes() {
      return Mob.createMobAttributes().add(Attributes.TEMPT_RANGE, 10.0);
   }

   @Override
   protected void customServerAiStep(final ServerLevel level) {
      if (this.getAge() != 0) {
         this.inLove = 0;
      }

      super.customServerAiStep(level);
   }

   @Override
   public void aiStep() {
      super.aiStep();
      if (this.getAge() != 0) {
         this.inLove = 0;
      }

      if (this.inLove > 0) {
         this.inLove--;
         if (this.inLove % 10 == 0) {
            double xa = this.random.nextGaussian() * 0.02;
            double ya = this.random.nextGaussian() * 0.02;
            double za = this.random.nextGaussian() * 0.02;
            this.level().addParticle(ParticleTypes.HEART, this.getRandomX(1.0), this.getRandomY() + 0.5, this.getRandomZ(1.0), xa, ya, za);
         }
      }
   }

   @Override
   protected void actuallyHurt(final ServerLevel level, final DamageSource source, final float dmg) {
      this.resetLove();
      super.actuallyHurt(level, source, dmg);
   }

   @Override
   public float getWalkTargetValue(final BlockPos pos, final LevelReader level) {
      return level.getBlockState(pos.below()).is(Blocks.GRASS_BLOCK) ? 10.0F : level.getPathfindingCostFromLightLevels(pos);
   }

   @Override
   protected void addAdditionalSaveData(final ValueOutput output) {
      super.addAdditionalSaveData(output);
      output.putInt("InLove", this.inLove);
      EntityReference.store(this.loveCause, output, "LoveCause");
   }

   @Override
   protected void readAdditionalSaveData(final ValueInput input) {
      super.readAdditionalSaveData(input);
      this.inLove = input.getIntOr("InLove", 0);
      this.loveCause = EntityReference.read(input, "LoveCause");
   }

   public static boolean checkAnimalSpawnRules(
      final EntityType<? extends Animal> type, final LevelAccessor level, final EntitySpawnReason spawnReason, final BlockPos pos, final RandomSource random
   ) {
      boolean brightEnoughToSpawn = EntitySpawnReason.ignoresLightRequirements(spawnReason) || isBrightEnoughToSpawn(level, pos);
      return level.getBlockState(pos.below()).is(BlockTags.ANIMALS_SPAWNABLE_ON) && brightEnoughToSpawn;
   }

   protected static boolean isBrightEnoughToSpawn(final BlockAndLightGetter level, final BlockPos pos) {
      return level.getRawBrightness(pos, 0) > 8;
   }

   @Override
   public int getAmbientSoundInterval() {
      return 120;
   }

   @Override
   public boolean removeWhenFarAway(final double distSqr) {
      return false;
   }

   @Override
   protected int getBaseExperienceReward(final ServerLevel level) {
      return 1 + this.random.nextInt(3);
   }

   public abstract boolean isFood(final ItemStack itemStack);

   @Override
   public InteractionResult mobInteract(final Player player, final InteractionHand hand) {
      ItemStack itemStack = player.getItemInHand(hand);
      if (this.isFood(itemStack)) {
         int age = this.getAge();
         if (player instanceof ServerPlayer serverPlayer && age == 0 && this.canFallInLove()) {
            this.usePlayerItem(player, hand, itemStack);
            this.setInLove(serverPlayer);
            this.playEatingSound();
            return InteractionResult.SUCCESS_SERVER;
         }

         if (this.canAgeUp()) {
            this.usePlayerItem(player, hand, itemStack);
            this.ageUp(getSpeedUpSecondsWhenFeeding(-age), true);
            this.playEatingSound();
            return InteractionResult.SUCCESS;
         }

         if (this.level().isClientSide()) {
            return InteractionResult.CONSUME;
         }
      }

      return super.mobInteract(player, hand);
   }

   protected void playEatingSound() {
   }

   public boolean canFallInLove() {
      return this.inLove <= 0;
   }

   public void setInLove(final @Nullable Player player) {
      this.inLove = 600;
      if (player instanceof ServerPlayer serverPlayer) {
         this.loveCause = EntityReference.of(serverPlayer);
      }

      this.level().broadcastEntityEvent(this, (byte)18);
   }

   public void setInLoveTime(final int time) {
      this.inLove = time;
   }

   public int getInLoveTime() {
      return this.inLove;
   }

   public @Nullable ServerPlayer getLoveCause() {
      return EntityReference.get(this.loveCause, this.level(), ServerPlayer.class);
   }

   public boolean isInLove() {
      return this.inLove > 0;
   }

   public void resetLove() {
      this.inLove = 0;
   }

   public boolean canMate(final Animal partner) {
      if (partner == this) {
         return false;
      } else {
         return partner.getClass() != this.getClass() ? false : this.isInLove() && partner.isInLove();
      }
   }

   public void spawnChildFromBreeding(final ServerLevel level, final Animal partner) {
      AgeableMob offspring = this.getBreedOffspring(level, partner);
      if (offspring != null) {
         offspring.setBaby(true);
         offspring.snapTo(this.getX(), this.getY(), this.getZ(), 0.0F, 0.0F);
         this.finalizeSpawnChildFromBreeding(level, partner, offspring);
         level.addFreshEntityWithPassengers(offspring);
      }
   }

   public void finalizeSpawnChildFromBreeding(final ServerLevel level, final Animal partner, final @Nullable AgeableMob offspring) {
      Optional.ofNullable(this.getLoveCause()).or(() -> Optional.ofNullable(partner.getLoveCause())).ifPresent(cause -> {
         cause.awardStat(Stats.ANIMALS_BRED);
         CriteriaTriggers.BRED_ANIMALS.trigger(cause, this, partner, offspring);
      });
      this.setAge(6000);
      partner.setAge(6000);
      this.resetLove();
      partner.resetLove();
      level.broadcastEntityEvent(this, (byte)18);
      if (level.getGameRules().get(GameRules.MOB_DROPS)) {
         level.addFreshEntity(new ExperienceOrb(level, this.getX(), this.getY(), this.getZ(), this.getRandom().nextInt(7) + 1));
      }
   }

   @Override
   public void handleEntityEvent(final byte id) {
      if (id == 18) {
         for (int i = 0; i < 7; i++) {
            double xa = this.random.nextGaussian() * 0.02;
            double ya = this.random.nextGaussian() * 0.02;
            double za = this.random.nextGaussian() * 0.02;
            this.level().addParticle(ParticleTypes.HEART, this.getRandomX(1.0), this.getRandomY() + 0.5, this.getRandomZ(1.0), xa, ya, za);
         }
      } else {
         super.handleEntityEvent(id);
      }
   }

   @Override
   public Vec3 getDismountLocationForPassenger(final LivingEntity passenger) {
      Direction forward = this.getMotionDirection();
      if (forward.getAxis() == Direction.Axis.Y) {
         return super.getDismountLocationForPassenger(passenger);
      }

      int[][] offsets = DismountHelper.offsetsForDirection(forward);
      BlockPos vehicleBlockPos = this.blockPosition();
      BlockPos.MutableBlockPos targetBlockPos = new BlockPos.MutableBlockPos();
      UnmodifiableIterator var6 = passenger.getDismountPoses().iterator();

      while (var6.hasNext()) {
         Pose dismountPose = (Pose)var6.next();
         AABB poseCollisionBox = passenger.getLocalBoundsForPose(dismountPose);

         for (int[] offsetXZ : offsets) {
            targetBlockPos.set(vehicleBlockPos.getX() + offsetXZ[0], vehicleBlockPos.getY(), vehicleBlockPos.getZ() + offsetXZ[1]);
            double blockFloorHeight = this.level().getBlockFloorHeight(targetBlockPos);
            if (DismountHelper.isBlockFloorValid(blockFloorHeight)) {
               Vec3 location = Vec3.upFromBottomCenterOf(targetBlockPos, blockFloorHeight);
               if (DismountHelper.canDismountTo(this.level(), passenger, poseCollisionBox.move(location))) {
                  passenger.setPose(dismountPose);
                  return location;
               }
            }
         }
      }

      return super.getDismountLocationForPassenger(passenger);
   }
}
