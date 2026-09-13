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
import net.minecraft.world.Difficulty;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
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

   public CreakingHeartBlockEntity(final BlockPos worldPosition, final BlockState blockState) {
      super(BlockEntityTypes.CREAKING_HEART, worldPosition, blockState);
   }

   public static void serverTick(final Level level, final BlockPos pos, final BlockState state, final CreakingHeartBlockEntity entity) {
      entity.ticksExisted++;
      if (level instanceof ServerLevel serverLevel) {
         int computedOutputSignal = entity.computeAnalogOutputSignal();
         if (entity.outputSignal != computedOutputSignal) {
            entity.outputSignal = computedOutputSignal;
            level.updateNeighbourForOutputSignal(pos, Blocks.CREAKING_HEART);
         }

         if (entity.emitter > 0) {
            if (entity.emitter > 50) {
               entity.emitParticles(serverLevel, 1, true);
               entity.emitParticles(serverLevel, 1, false);
            }

            if (entity.emitter % 10 == 0 && entity.emitterTarget != null) {
               entity.getCreakingProtector().ifPresent(creakingx -> entity.emitterTarget = creakingx.getBoundingBox().getCenter());
               Vec3 heartPosition = Vec3.atCenterOf(pos);
               float progress = 0.2F + 0.8F * (100 - entity.emitter) / 100.0F;
               Vec3 soundLocation = heartPosition.subtract(entity.emitterTarget).scale(progress).add(entity.emitterTarget);
               BlockPos soundPos = BlockPos.containing(soundLocation);
               float volume = entity.emitter / 2.0F / 100.0F + 0.5F;
               serverLevel.playSound(null, soundPos, SoundEvents.CREAKING_HEART_HURT, SoundSource.BLOCKS, volume, 1.0F);
            }

            entity.emitter--;
         }

         if (entity.ticker-- < 0) {
            entity.ticker = entity.level == null ? 20 : entity.level.getRandom().nextInt(5) + 20;
            BlockState updatedState = updateCreakingState(level, state, pos, entity);
            if (updatedState != state) {
               level.setBlockAndUpdate(pos, updatedState);
               if (updatedState.getValue(CreakingHeartBlock.STATE) == CreakingHeartState.UPROOTED) {
                  return;
               }
            }

            if (entity.creakingInfo == null) {
               if (updatedState.getValue(CreakingHeartBlock.STATE) == CreakingHeartState.AWAKE) {
                  if (serverLevel.isSpawningMonsters() && serverLevel.getLevelData().getDifficulty() != Difficulty.PEACEFUL) {
                     Player player = level.getNearestPlayer(pos.getX(), pos.getY(), pos.getZ(), 32.0, false);
                     if (player != null) {
                        Creaking creaking = spawnProtector(serverLevel, entity);
                        if (creaking != null) {
                           entity.setCreakingInfo(creaking);
                           creaking.makeSound(SoundEvents.CREAKING_SPAWN);
                           level.playSound(null, entity.getBlockPos(), SoundEvents.CREAKING_HEART_SPAWN, SoundSource.BLOCKS, 1.0F, 1.0F);
                        }
                     }
                  }
               }
            } else {
               Optional<Creaking> optionalCreaking = entity.getCreakingProtector();
               if (optionalCreaking.isPresent()) {
                  Creaking creaking = optionalCreaking.get();
                  if (!level.environmentAttributes().getValue(EnvironmentAttributes.CREAKING_ACTIVE, pos) && !creaking.isPersistenceRequired()
                     || entity.distanceToCreaking() > 34.0
                     || creaking.playerIsStuckInYou()) {
                     entity.removeProtector(null);
                  }
               }
            }
         }
      }
   }

   private static BlockState updateCreakingState(final Level level, final BlockState state, final BlockPos pos, final CreakingHeartBlockEntity entity) {
      if (!CreakingHeartBlock.hasRequiredLogs(state, level, pos) && entity.creakingInfo == null) {
         return state.setValue(CreakingHeartBlock.STATE, CreakingHeartState.UPROOTED);
      }

      CreakingHeartState heartState = level.environmentAttributes().getValue(EnvironmentAttributes.CREAKING_ACTIVE, pos)
         ? CreakingHeartState.AWAKE
         : CreakingHeartState.DORMANT;
      return state.setValue(CreakingHeartBlock.STATE, heartState);
   }

   private double distanceToCreaking() {
      return this.getCreakingProtector().map(creaking -> Math.sqrt(creaking.distanceToSqr(Vec3.atBottomCenterOf(this.getBlockPos())))).orElse(0.0);
   }

   private void clearCreakingInfo() {
      this.creakingInfo = null;
      this.setChanged();
   }

   public void setCreakingInfo(final Creaking creaking) {
      this.creakingInfo = Either.left(creaking);
      this.setChanged();
   }

   public void setCreakingInfo(final UUID uuid) {
      this.creakingInfo = Either.right(uuid);
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

      if (this.level instanceof ServerLevel serverLevel && this.creakingInfo.right().isPresent()) {
         UUID uuid = (UUID)this.creakingInfo.right().get();
         if (serverLevel.getEntity(uuid) instanceof Creaking resolvedCreaking) {
            this.setCreakingInfo(resolvedCreaking);
            return Optional.of(resolvedCreaking);
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

   private static @Nullable Creaking spawnProtector(final ServerLevel level, final CreakingHeartBlockEntity entity) {
      BlockPos pos = entity.getBlockPos();
      Optional<Creaking> spawnedMob = SpawnUtil.trySpawnMob(
         EntityTypes.CREAKING, EntitySpawnReason.SPAWNER, level, pos, 5, 16, 8, SpawnUtil.Strategy.ON_TOP_OF_COLLIDER_NO_LEAVES, true
      );
      if (spawnedMob.isEmpty()) {
         return null;
      }

      Creaking spawnedCreaking = spawnedMob.get();
      level.gameEvent(spawnedCreaking, GameEvent.ENTITY_PLACE, spawnedCreaking.position());
      level.broadcastEntityEvent(spawnedCreaking, (byte)60);
      spawnedCreaking.setTransient(pos);
      return spawnedCreaking;
   }

   public ClientboundBlockEntityDataPacket getUpdatePacket() {
      return ClientboundBlockEntityDataPacket.create(this);
   }

   @Override
   public CompoundTag getUpdateTag(final HolderLookup.Provider registries) {
      return this.saveCustomOnly(registries);
   }

   public void creakingHurt() {
      Optional<Creaking> creaking = this.getCreakingProtector();
      if (!creaking.isEmpty()) {
         if (this.level instanceof ServerLevel serverLevel) {
            if (this.emitter <= 0) {
               this.emitParticles(serverLevel, 20, false);
               if (this.getBlockState().getValue(CreakingHeartBlock.STATE) == CreakingHeartState.AWAKE) {
                  int numberOfClumps = this.level.getRandom().nextIntBetweenInclusive(2, 3);

                  for (int i = 0; i < numberOfClumps; i++) {
                     this.spreadResin(serverLevel).ifPresent(blockPos -> {
                        this.level.playSound(null, blockPos, SoundEvents.RESIN_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);
                        this.level.gameEvent(GameEvent.BLOCK_PLACE, blockPos, GameEvent.Context.of(this.getBlockState()));
                     });
                  }
               }

               this.emitter = 100;
               this.emitterTarget = creaking.get().getBoundingBox().getCenter();
            }
         }
      }
   }

   private Optional<BlockPos> spreadResin(final ServerLevel level) {
      RandomSource random = level.getRandom();
      Mutable<BlockPos> placedResin = new MutableObject(null);
      BlockPos.breadthFirstTraversal(this.worldPosition, 2, 64, (pos, acceptor) -> {
         for (Direction dir : Util.shuffledCopy(Direction.values(), random)) {
            BlockPos neighbourPos = pos.relative(dir);
            if (level.getBlockState(neighbourPos).is(BlockTags.PALE_OAK_LOGS)) {
               acceptor.accept(neighbourPos);
            }
         }
      }, pos -> {
         if (!level.getBlockState(pos).is(BlockTags.PALE_OAK_LOGS)) {
            return BlockPos.TraversalNodeStatus.ACCEPT;
         }

         for (Direction dir : Util.shuffledCopy(Direction.values(), random)) {
            BlockPos neightbourPos = pos.relative(dir);
            BlockState neighbourState = level.getBlockState(neightbourPos);
            Direction opposite = dir.getOpposite();
            if (neighbourState.isAir()) {
               neighbourState = Blocks.RESIN_CLUMP.defaultBlockState();
            } else if (neighbourState.is(Blocks.WATER) && neighbourState.getFluidState().isSource()) {
               neighbourState = Blocks.RESIN_CLUMP.defaultBlockState().setValue(MultifaceBlock.WATERLOGGED, true);
            }

            if (neighbourState.is(Blocks.RESIN_CLUMP) && !MultifaceBlock.hasFace(neighbourState, opposite)) {
               level.setBlockAndUpdate(neightbourPos, neighbourState.setValue(MultifaceBlock.getFaceProperty(opposite), true));
               placedResin.setValue(neightbourPos);
               return BlockPos.TraversalNodeStatus.STOP;
            }
         }

         return BlockPos.TraversalNodeStatus.ACCEPT;
      });
      return Optional.ofNullable((BlockPos)placedResin.get());
   }

   private void emitParticles(final ServerLevel serverLevel, final int count, final boolean towardsCreaking) {
      Optional<Creaking> creaking = this.getCreakingProtector();
      if (!creaking.isEmpty()) {
         int color = towardsCreaking ? 16545810 : 6250335;
         RandomSource random = serverLevel.getRandom();

         for (double i = 0.0; i < count; i++) {
            AABB box = creaking.get().getBoundingBox();
            Vec3 source = box.getMinPosition()
               .add(random.nextDouble() * box.getXsize(), random.nextDouble() * box.getYsize(), random.nextDouble() * box.getZsize());
            Vec3 destination = Vec3.atLowerCornerOf(this.getBlockPos()).add(random.nextDouble(), random.nextDouble(), random.nextDouble());
            if (towardsCreaking) {
               Vec3 foo = source;
               source = destination;
               destination = foo;
            }

            TrailParticleOption particleOption = new TrailParticleOption(destination, color, random.nextInt(40) + 10);
            serverLevel.sendParticles(particleOption, true, true, source.x, source.y, source.z, 1, 0.0, 0.0, 0.0, 0.0);
         }
      }
   }

   @Override
   public void preRemoveSideEffects(final BlockPos pos, final BlockState state) {
      this.removeProtector(null);
   }

   public void removeProtector(final @Nullable DamageSource damageSource) {
      Optional<Creaking> creakingProtector = this.getCreakingProtector();
      if (creakingProtector.isPresent()) {
         Creaking creaking = creakingProtector.get();
         if (damageSource == null) {
            creaking.tearDown();
         } else {
            creaking.creakingDeathEffects(damageSource);
            creaking.setTearingDown();
            creaking.setHealth(0.0F);
         }

         this.clearCreakingInfo();
      }
   }

   public boolean isProtector(final Creaking creaking) {
      return this.getCreakingProtector().map(c -> c == creaking).orElse(false);
   }

   public int getAnalogOutputSignal() {
      return this.outputSignal;
   }

   public int computeAnalogOutputSignal() {
      if (this.creakingInfo != null && !this.getCreakingProtector().isEmpty()) {
         double distance = this.distanceToCreaking();
         double scaledDistance = Math.clamp(distance, 0.0, 32.0) / 32.0;
         return 15 - (int)Math.floor(scaledDistance * 15.0);
      } else {
         return 0;
      }
   }

   @Override
   protected void loadAdditional(final ValueInput input) {
      super.loadAdditional(input);
      input.<UUID>read("creaking", UUIDUtil.CODEC).ifPresentOrElse(this::setCreakingInfo, this::clearCreakingInfo);
   }

   @Override
   protected void saveAdditional(final ValueOutput output) {
      super.saveAdditional(output);
      if (this.creakingInfo != null) {
         output.store("creaking", UUIDUtil.CODEC, (UUID)this.creakingInfo.map(Entity::getUUID, uuid -> uuid));
      }
   }
}
