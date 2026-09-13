package net.minecraft.world.level.block.entity;

import com.mojang.datafixers.util.Either;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.particles.TrailParticleOption;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.util.SpawnUtil;
import net.minecraft.util.Util;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.creaking.Creaking;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CreakingHeartBlock;
import net.minecraft.world.level.block.MultifaceBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.CreakingHeartState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.mutable.Mutable;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jspecify.annotations.Nullable;

public class CreakingHeartBlockEntity extends BlockEntity {
   private static final int PLAYER_DETECTION_RANGE = 32;
   public static final int CREAKING_ROAMING_RADIUS = 32;
   private static final int DISTANCE_CREAKING_TOO_FAR = 34;
   private static final int SPAWN_RANGE_XZ = 16;
   private static final int SPAWN_RANGE_Y = 8;
   private static final int ATTEMPTS_PER_SPAWN = 5;
   private static final int UPDATE_TICKS = 20;
   private static final int UPDATE_TICKS_VARIANCE = 5;
   private static final int HURT_CALL_TOTAL_TICKS = 100;
   private static final int NUMBER_OF_HURT_CALLS = 10;
   private static final int HURT_CALL_INTERVAL = 10;
   private static final int HURT_CALL_PARTICLE_TICKS = 50;
   private static final int MAX_DEPTH = 2;
   private static final int MAX_COUNT = 64;
   private static final int TICKS_GRACE_PERIOD = 30;
   private static final Optional<Creaking> NO_CREAKING = Optional.empty();
   private @Nullable Either<Creaking, UUID> creakingInfo;
   private long ticksExisted;
   private int ticker;
   private int emitter;
   private @Nullable Vec3 emitterTarget;
   private int outputSignal;

   public CreakingHeartBlockEntity(BlockPos p_369235_, BlockState p_367834_) {
      super(BlockEntityType.CREAKING_HEART, p_369235_, p_367834_);
   }

   public static void serverTick(Level p_360952_, BlockPos p_367184_, BlockState p_365574_, CreakingHeartBlockEntity p_366884_) {
      p_366884_.ticksExisted++;
      if (p_360952_ instanceof ServerLevel serverlevel) {
         int $$6 = p_366884_.computeAnalogOutputSignal();
         if (p_366884_.outputSignal != $$6) {
            p_366884_.outputSignal = $$6;
            p_360952_.updateNeighbourForOutputSignal(p_367184_, Blocks.CREAKING_HEART);
         }

         if (p_366884_.emitter > 0) {
            if (p_366884_.emitter > 50) {
               p_366884_.emitParticles(serverlevel, 1, true);
               p_366884_.emitParticles(serverlevel, 1, false);
            }

            if (p_366884_.emitter % 10 == 0 && p_366884_.emitterTarget != null) {
               p_366884_.getCreakingProtector().ifPresent(p_449924_ -> p_366884_.emitterTarget = p_449924_.getBoundingBox().getCenter());
               Vec3 vec3 = Vec3.atCenterOf(p_367184_);
               float f = 0.2F + 0.8F * (100 - p_366884_.emitter) / 100.0F;
               Vec3 vec31 = vec3.subtract(p_366884_.emitterTarget).scale(f).add(p_366884_.emitterTarget);
               BlockPos blockpos = BlockPos.containing(vec31);
               float f1 = p_366884_.emitter / 2.0F / 100.0F + 0.5F;
               serverlevel.playSound(null, blockpos, SoundEvents.CREAKING_HEART_HURT, SoundSource.BLOCKS, f1, 1.0F);
            }

            p_366884_.emitter--;
         }

         if (p_366884_.ticker-- < 0) {
            p_366884_.ticker = p_366884_.level == null ? 20 : p_366884_.level.random.nextInt(5) + 20;
            BlockState blockstate = updateCreakingState(p_360952_, p_365574_, p_367184_, p_366884_);
            if (blockstate != p_365574_) {
               p_360952_.setBlock(p_367184_, blockstate, 3);
               if (blockstate.getValue(CreakingHeartBlock.STATE) == CreakingHeartState.UPROOTED) {
                  return;
               }
            }

            if (p_366884_.creakingInfo == null) {
               if (blockstate.getValue(CreakingHeartBlock.STATE) == CreakingHeartState.AWAKE && serverlevel.isSpawningMonsters()) {
                  Player player = p_360952_.getNearestPlayer(p_367184_.getX(), p_367184_.getY(), p_367184_.getZ(), 32.0, false);
                  if (player != null) {
                     Creaking creaking1 = spawnProtector(serverlevel, p_366884_);
                     if (creaking1 != null) {
                        p_366884_.setCreakingInfo(creaking1);
                        creaking1.makeSound(SoundEvents.CREAKING_SPAWN);
                        p_360952_.playSound(null, p_366884_.getBlockPos(), SoundEvents.CREAKING_HEART_SPAWN, SoundSource.BLOCKS, 1.0F, 1.0F);
                     }
                  }
               }
            } else {
               Optional<Creaking> optional = p_366884_.getCreakingProtector();
               if (optional.isPresent()) {
                  Creaking creaking = optional.get();
                  if (!p_360952_.environmentAttributes().getValue(EnvironmentAttributes.CREAKING_ACTIVE, p_367184_) && !creaking.isPersistenceRequired()
                     || p_366884_.distanceToCreaking() > 34.0
                     || creaking.playerIsStuckInYou()) {
                     p_366884_.removeProtector(null);
                  }
               }
            }
         }
      }
   }

   private static BlockState updateCreakingState(Level p_391469_, BlockState p_392285_, BlockPos p_394743_, CreakingHeartBlockEntity p_397498_) {
      if (!CreakingHeartBlock.hasRequiredLogs(p_392285_, p_391469_, p_394743_) && p_397498_.creakingInfo == null) {
         return p_392285_.setValue(CreakingHeartBlock.STATE, CreakingHeartState.UPROOTED);
      }

      CreakingHeartState creakingheartstate = p_391469_.environmentAttributes().getValue(EnvironmentAttributes.CREAKING_ACTIVE, p_394743_)
         ? CreakingHeartState.AWAKE
         : CreakingHeartState.DORMANT;
      return p_392285_.setValue(CreakingHeartBlock.STATE, creakingheartstate);
   }

   private double distanceToCreaking() {
      return this.getCreakingProtector().map(p_422157_ -> Math.sqrt(p_422157_.distanceToSqr(Vec3.atBottomCenterOf(this.getBlockPos())))).orElse(0.0);
   }

   private void clearCreakingInfo() {
      this.creakingInfo = null;
      this.setChanged();
   }

   public void setCreakingInfo(Creaking p_376531_) {
      this.creakingInfo = Either.left(p_376531_);
      this.setChanged();
   }

   public void setCreakingInfo(UUID p_376550_) {
      this.creakingInfo = Either.right(p_376550_);
      this.ticksExisted = 0L;
      this.setChanged();
   }

   private Optional<Creaking> getCreakingProtector() {
      if (this.creakingInfo == null) {
         return NO_CREAKING;
      }

      if (this.creakingInfo.left().isPresent()) {
         Creaking creaking = (Creaking)this.creakingInfo.left().get();
         if (!creaking.isRemoved()) {
            return Optional.of(creaking);
         }

         this.setCreakingInfo(creaking.getUUID());
      }

      if (this.level instanceof ServerLevel serverlevel && this.creakingInfo.right().isPresent()) {
         UUID uuid = (UUID)this.creakingInfo.right().get();
         if (serverlevel.getEntity(uuid) instanceof Creaking creaking1) {
            this.setCreakingInfo(creaking1);
            return Optional.of(creaking1);
         } else {
            if (this.ticksExisted >= 30L) {
               this.clearCreakingInfo();
            }

            return NO_CREAKING;
         }
      } else {
         return NO_CREAKING;
      }
   }

   private static @Nullable Creaking spawnProtector(ServerLevel p_362442_, CreakingHeartBlockEntity p_369130_) {
      BlockPos blockpos = p_369130_.getBlockPos();
      Optional<Creaking> optional = SpawnUtil.trySpawnMob(
         EntityType.CREAKING, EntitySpawnReason.SPAWNER, p_362442_, blockpos, 5, 16, 8, SpawnUtil.Strategy.ON_TOP_OF_COLLIDER_NO_LEAVES, true
      );
      if (optional.isEmpty()) {
         return null;
      }

      Creaking creaking = optional.get();
      p_362442_.gameEvent(creaking, GameEvent.ENTITY_PLACE, creaking.position());
      p_362442_.broadcastEntityEvent(creaking, (byte)60);
      creaking.setTransient(blockpos);
      return creaking;
   }

   public ClientboundBlockEntityDataPacket getUpdatePacket() {
      return ClientboundBlockEntityDataPacket.create(this);
   }

   @Override
   public CompoundTag getUpdateTag(HolderLookup.Provider p_366353_) {
      return this.saveCustomOnly(p_366353_);
   }

   public void creakingHurt() {
      if (this.getCreakingProtector().orElse(null) instanceof Creaking creaking && this.level instanceof ServerLevel serverlevel && this.emitter <= 0) {
         this.emitParticles(serverlevel, 20, false);
         if (this.getBlockState().getValue(CreakingHeartBlock.STATE) == CreakingHeartState.AWAKE) {
            int j = this.level.getRandom().nextIntBetweenInclusive(2, 3);

            for (int i = 0; i < j; i++) {
               this.spreadResin(serverlevel).ifPresent(p_422158_ -> {
                  this.level.playSound(null, p_422158_, SoundEvents.RESIN_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);
                  this.level.gameEvent(GameEvent.BLOCK_PLACE, p_422158_, GameEvent.Context.of(this.getBlockState()));
               });
            }
         }

         this.emitter = 100;
         this.emitterTarget = creaking.getBoundingBox().getCenter();
      }
   }

   private Optional<BlockPos> spreadResin(ServerLevel p_457610_) {
      Mutable<BlockPos> mutable = new MutableObject(null);
      BlockPos.breadthFirstTraversal(this.worldPosition, 2, 64, (p_449921_, p_449922_) -> {
         for (Direction direction : Util.shuffledCopy(Direction.values(), p_457610_.random)) {
            BlockPos blockpos = p_449921_.relative(direction);
            if (p_457610_.getBlockState(blockpos).is(BlockTags.PALE_OAK_LOGS)) {
               p_449922_.accept(blockpos);
            }
         }
      }, p_449919_ -> {
         if (!p_457610_.getBlockState(p_449919_).is(BlockTags.PALE_OAK_LOGS)) {
            return BlockPos.TraversalNodeStatus.ACCEPT;
         }

         for (Direction direction : Util.shuffledCopy(Direction.values(), p_457610_.random)) {
            BlockPos blockpos = p_449919_.relative(direction);
            BlockState blockstate = p_457610_.getBlockState(blockpos);
            Direction direction1 = direction.getOpposite();
            if (blockstate.isAir()) {
               blockstate = Blocks.RESIN_CLUMP.defaultBlockState();
            } else if (blockstate.is(Blocks.WATER) && blockstate.getFluidState().isSource()) {
               blockstate = Blocks.RESIN_CLUMP.defaultBlockState().setValue(MultifaceBlock.WATERLOGGED, true);
            }

            if (blockstate.is(Blocks.RESIN_CLUMP) && !MultifaceBlock.hasFace(blockstate, direction1)) {
               p_457610_.setBlock(blockpos, blockstate.setValue(MultifaceBlock.getFaceProperty(direction1), true), 3);
               mutable.setValue(blockpos);
               return BlockPos.TraversalNodeStatus.STOP;
            }
         }

         return BlockPos.TraversalNodeStatus.ACCEPT;
      });
      return Optional.ofNullable((BlockPos)mutable.get());
   }

   private void emitParticles(ServerLevel p_366930_, int p_366541_, boolean p_366282_) {
      if (this.getCreakingProtector().orElse(null) instanceof Creaking creaking) {
         int i = p_366282_ ? 16545810 : 6250335;
         RandomSource randomsource = p_366930_.random;

         for (double d0 = 0.0; d0 < p_366541_; d0++) {
            AABB aabb = creaking.getBoundingBox();
            Vec3 vec3 = aabb.getMinPosition()
               .add(randomsource.nextDouble() * aabb.getXsize(), randomsource.nextDouble() * aabb.getYsize(), randomsource.nextDouble() * aabb.getZsize());
            Vec3 vec31 = Vec3.atLowerCornerOf(this.getBlockPos()).add(randomsource.nextDouble(), randomsource.nextDouble(), randomsource.nextDouble());
            if (p_366282_) {
               Vec3 vec32 = vec3;
               vec3 = vec31;
               vec31 = vec32;
            }

            TrailParticleOption trailparticleoption = new TrailParticleOption(vec31, i, randomsource.nextInt(40) + 10);
            p_366930_.sendParticles(trailparticleoption, true, true, vec3.x, vec3.y, vec3.z, 1, 0.0, 0.0, 0.0, 0.0);
         }
      }
   }

   @Override
   public void preRemoveSideEffects(BlockPos p_395687_, BlockState p_393763_) {
      this.removeProtector(null);
   }

   public void removeProtector(@Nullable DamageSource p_364053_) {
      if (this.getCreakingProtector().orElse(null) instanceof Creaking creaking) {
         if (p_364053_ == null) {
            creaking.tearDown();
         } else {
            creaking.creakingDeathEffects(p_364053_);
            creaking.setTearingDown();
            creaking.setHealth(0.0F);
         }

         this.clearCreakingInfo();
      }
   }

   public boolean isProtector(Creaking p_367915_) {
      return this.getCreakingProtector().map(p_375974_ -> p_375974_ == p_367915_).orElse(false);
   }

   public int getAnalogOutputSignal() {
      return this.outputSignal;
   }

   public int computeAnalogOutputSignal() {
      if (this.creakingInfo != null && !this.getCreakingProtector().isEmpty()) {
         double d0 = this.distanceToCreaking();
         double d1 = Math.clamp(d0, 0.0, 32.0) / 32.0;
         return 15 - (int)Math.floor(d1 * 15.0);
      } else {
         return 0;
      }
   }

   @Override
   protected void loadAdditional(ValueInput p_407387_) {
      super.loadAdditional(p_407387_);
      p_407387_.<UUID>read("creaking", UUIDUtil.CODEC).ifPresentOrElse(this::setCreakingInfo, this::clearCreakingInfo);
   }

   @Override
   protected void saveAdditional(ValueOutput p_408305_) {
      super.saveAdditional(p_408305_);
      if (this.creakingInfo != null) {
         p_408305_.store("creaking", UUIDUtil.CODEC, (UUID)this.creakingInfo.map(Entity::getUUID, p_377652_ -> p_377652_));
      }
   }
}
