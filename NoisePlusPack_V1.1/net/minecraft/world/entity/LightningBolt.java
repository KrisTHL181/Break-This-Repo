package net.minecraft.world.entity;

import com.google.common.collect.Sets;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.item.HoneycombItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.LightningRodBlock;
import net.minecraft.world.level.block.WeatheringCopper;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class LightningBolt extends Entity {
   private static final int START_LIFE = 2;
   private static final double DAMAGE_RADIUS = 3.0;
   private static final double DETECTION_RADIUS = 15.0;
   private int life;
   public long seed;
   private int flashes;
   private boolean visualOnly;
   private @Nullable ServerPlayer cause;
   private final Set<Entity> hitEntities = Sets.newHashSet();
   private int blocksSetOnFire;

   public LightningBolt(EntityType<? extends LightningBolt> p_20865_, Level p_20866_) {
      super(p_20865_, p_20866_);
      this.life = 2;
      this.seed = this.random.nextLong();
      this.flashes = this.random.nextInt(3) + 1;
   }

   public void setVisualOnly(boolean p_20875_) {
      this.visualOnly = p_20875_;
   }

   @Override
   public SoundSource getSoundSource() {
      return SoundSource.WEATHER;
   }

   public @Nullable ServerPlayer getCause() {
      return this.cause;
   }

   public void setCause(@Nullable ServerPlayer p_20880_) {
      this.cause = p_20880_;
   }

   private void powerLightningRod() {
      BlockPos blockpos = this.getStrikePosition();
      BlockState blockstate = this.level().getBlockState(blockpos);
      if (blockstate.getBlock() instanceof LightningRodBlock lightningrodblock) {
         lightningrodblock.onLightningStrike(blockstate, this.level(), blockpos);
      }
   }

   @Override
   public void tick() {
      super.tick();
      if (this.life == 2) {
         if (this.level().isClientSide()) {
            this.level()
               .playLocalSound(
                  this.getX(),
                  this.getY(),
                  this.getZ(),
                  SoundEvents.LIGHTNING_BOLT_THUNDER,
                  SoundSource.WEATHER,
                  10000.0F,
                  0.8F + this.random.nextFloat() * 0.2F,
                  false
               );
            this.level()
               .playLocalSound(
                  this.getX(),
                  this.getY(),
                  this.getZ(),
                  SoundEvents.LIGHTNING_BOLT_IMPACT,
                  SoundSource.WEATHER,
                  2.0F,
                  0.5F + this.random.nextFloat() * 0.2F,
                  false
               );
         } else {
            Difficulty difficulty = this.level().getDifficulty();
            if (difficulty == Difficulty.NORMAL || difficulty == Difficulty.HARD) {
               this.spawnFire(4);
            }

            this.powerLightningRod();
            clearCopperOnLightningStrike(this.level(), this.getStrikePosition());
            this.gameEvent(GameEvent.LIGHTNING_STRIKE);
         }
      }

      this.life--;
      if (this.life < 0) {
         if (this.flashes == 0) {
            if (this.level() instanceof ServerLevel) {
               List<Entity> list = this.level()
                  .getEntities(
                     this,
                     new AABB(this.getX() - 15.0, this.getY() - 15.0, this.getZ() - 15.0, this.getX() + 15.0, this.getY() + 6.0 + 15.0, this.getZ() + 15.0),
                     p_147140_ -> p_147140_.isAlive() && !this.hitEntities.contains(p_147140_)
                  );

               for (ServerPlayer serverplayer : ((ServerLevel)this.level()).getPlayers(p_326774_ -> p_326774_.distanceTo(this) < 256.0F)) {
                  CriteriaTriggers.LIGHTNING_STRIKE.trigger(serverplayer, this, list);
               }
            }

            this.discard();
         } else if (this.life < -this.random.nextInt(10)) {
            this.flashes--;
            this.life = 1;
            this.seed = this.random.nextLong();
            this.spawnFire(0);
         }
      }

      if (this.life >= 0) {
         if (!(this.level() instanceof ServerLevel)) {
            this.level().setSkyFlashTime(2);
         } else if (!this.visualOnly) {
            List<Entity> list1 = this.level()
               .getEntities(
                  this,
                  new AABB(this.getX() - 3.0, this.getY() - 3.0, this.getZ() - 3.0, this.getX() + 3.0, this.getY() + 6.0 + 3.0, this.getZ() + 3.0),
                  Entity::isAlive
               );

            for (Entity entity : list1) {
               entity.thunderHit((ServerLevel)this.level(), this);
            }

            this.hitEntities.addAll(list1);
            if (this.cause != null) {
               CriteriaTriggers.CHANNELED_LIGHTNING.trigger(this.cause, list1);
            }
         }
      }
   }

   private BlockPos getStrikePosition() {
      Vec3 vec3 = this.position();
      return BlockPos.containing(vec3.x, vec3.y - 1.0E-6, vec3.z);
   }

   private void spawnFire(int p_20871_) {
      if (!this.visualOnly && this.level() instanceof ServerLevel serverlevel) {
         BlockPos blockpos1 = this.blockPosition();
         if (serverlevel.canSpreadFireAround(blockpos1)) {
            BlockState blockstate = BaseFireBlock.getState(serverlevel, blockpos1);
            if (serverlevel.getBlockState(blockpos1).isAir() && blockstate.canSurvive(serverlevel, blockpos1)) {
               serverlevel.setBlockAndUpdate(blockpos1, blockstate);
               this.blocksSetOnFire++;
            }

            for (int i = 0; i < p_20871_; i++) {
               BlockPos blockpos = blockpos1.offset(this.random.nextInt(3) - 1, this.random.nextInt(3) - 1, this.random.nextInt(3) - 1);
               blockstate = BaseFireBlock.getState(serverlevel, blockpos);
               if (serverlevel.getBlockState(blockpos).isAir() && blockstate.canSurvive(serverlevel, blockpos)) {
                  serverlevel.setBlockAndUpdate(blockpos, blockstate);
                  this.blocksSetOnFire++;
               }
            }
         }
      }
   }

   private static void clearCopperOnLightningStrike(Level p_147151_, BlockPos p_147152_) {
      BlockState blockstate = p_147151_.getBlockState(p_147152_);
      boolean flag = HoneycombItem.WAX_OFF_BY_BLOCK.get().get(blockstate.getBlock()) != null;
      boolean flag1 = blockstate.getBlock() instanceof WeatheringCopper;
      if (flag1 || flag) {
         if (flag1) {
            p_147151_.setBlockAndUpdate(p_147152_, WeatheringCopper.getFirst(p_147151_.getBlockState(p_147152_)));
         }

         BlockPos.MutableBlockPos blockpos$mutableblockpos = p_147152_.mutable();
         int i = p_147151_.random.nextInt(3) + 3;

         for (int j = 0; j < i; j++) {
            int k = p_147151_.random.nextInt(8) + 1;
            randomWalkCleaningCopper(p_147151_, p_147152_, blockpos$mutableblockpos, k);
         }
      }
   }

   private static void randomWalkCleaningCopper(Level p_147146_, BlockPos p_147147_, BlockPos.MutableBlockPos p_147148_, int p_147149_) {
      p_147148_.set(p_147147_);

      for (int i = 0; i < p_147149_; i++) {
         Optional<BlockPos> optional = randomStepCleaningCopper(p_147146_, p_147148_);
         if (optional.isEmpty()) {
            break;
         }

         p_147148_.set(optional.get());
      }
   }

   private static Optional<BlockPos> randomStepCleaningCopper(Level p_147154_, BlockPos p_147155_) {
      for (BlockPos blockpos : BlockPos.randomInCube(p_147154_.random, 10, p_147155_, 1)) {
         BlockState blockstate = p_147154_.getBlockState(blockpos);
         if (blockstate.getBlock() instanceof WeatheringCopper) {
            WeatheringCopper.getPrevious(blockstate).ifPresent(p_147144_ -> p_147154_.setBlockAndUpdate(blockpos, p_147144_));
            p_147154_.levelEvent(3002, blockpos, -1);
            return Optional.of(blockpos);
         }
      }

      return Optional.empty();
   }

   @Override
   public boolean shouldRenderAtSqrDistance(double p_20869_) {
      double d0 = 64.0 * getViewScale();
      return p_20869_ < d0 * d0;
   }

   @Override
   protected void defineSynchedData(SynchedEntityData.Builder p_336100_) {
   }

   @Override
   protected void readAdditionalSaveData(ValueInput p_406930_) {
   }

   @Override
   protected void addAdditionalSaveData(ValueOutput p_410429_) {
   }

   public int getBlocksSetOnFire() {
      return this.blocksSetOnFire;
   }

   public Stream<Entity> getHitEntities() {
      return this.hitEntities.stream().filter(Entity::isAlive);
   }

   @Override
   public final boolean hurtServer(ServerLevel p_368015_, DamageSource p_364945_, float p_364228_) {
      return false;
   }
}
