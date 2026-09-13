package net.minecraft.world.entity.item;

import com.mojang.logging.LogUtils;
import java.util.function.Predicate;
import net.minecraft.CrashReportCategory;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.MoveSimulationType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.DirectionalPlaceContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AnvilBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ConcretePowderBlock;
import net.minecraft.world.level.block.Fallable;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class FallingBlockEntity extends Entity {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final BlockState DEFAULT_BLOCK_STATE = Blocks.SAND.defaultBlockState();
   private static final int DEFAULT_TIME = 0;
   private static final float DEFAULT_FALL_DAMAGE_PER_DISTANCE = 0.0F;
   private static final int DEFAULT_MAX_FALL_DAMAGE = 40;
   private static final boolean DEFAULT_DROP_ITEM = true;
   private static final boolean DEFAULT_CANCEL_DROP = false;
   private BlockState blockState = DEFAULT_BLOCK_STATE;
   public int time = 0;
   public boolean dropItem = true;
   private boolean cancelDrop = false;
   private boolean hurtEntities;
   private int fallDamageMax = 40;
   private float fallDamagePerDistance = 0.0F;
   public @Nullable CompoundTag blockData;
   public boolean forceTickAfterTeleportToDuplicate;
   protected static final EntityDataAccessor<BlockPos> DATA_START_POS = SynchedEntityData.defineId(FallingBlockEntity.class, EntityDataSerializers.BLOCK_POS);

   public FallingBlockEntity(final EntityType<? extends FallingBlockEntity> type, final Level level) {
      super(type, level);
   }

   private FallingBlockEntity(final Level level, final double x, final double y, final double z, final BlockState blockState) {
      this(EntityTypes.FALLING_BLOCK, level);
      this.blockState = blockState;
      this.blocksBuilding = true;
      this.setPos(x, y, z);
      this.setDeltaMovement(Vec3.ZERO);
      this.xo = x;
      this.yo = y;
      this.zo = z;
      this.setStartPos(this.blockPosition());
   }

   public static FallingBlockEntity fall(final Level level, final BlockPos pos, final BlockState state) {
      FallingBlockEntity entity = new FallingBlockEntity(
         level,
         pos.getX() + 0.5,
         pos.getY(),
         pos.getZ() + 0.5,
         state.hasProperty(BlockStateProperties.WATERLOGGED) ? state.setValue(BlockStateProperties.WATERLOGGED, false) : state
      );
      level.setBlockAndUpdate(pos, state.getFluidState().createLegacyBlock());
      level.addFreshEntity(entity);
      return entity;
   }

   @Override
   public boolean isAttackable() {
      return false;
   }

   @Override
   public final boolean hurtServer(final ServerLevel level, final DamageSource source, final float damage) {
      if (!this.isInvulnerableToBase(source)) {
         this.markHurt();
      }

      return false;
   }

   public void setStartPos(final BlockPos pos) {
      this.entityData.set(DATA_START_POS, pos);
   }

   public BlockPos getStartPos() {
      return this.entityData.get(DATA_START_POS);
   }

   @Override
   protected Entity.MovementEmission getMovementEmission() {
      return Entity.MovementEmission.NONE;
   }

   @Override
   protected void defineSynchedData(final SynchedEntityData.Builder entityData) {
      entityData.define(DATA_START_POS, BlockPos.ZERO);
   }

   @Override
   public boolean isPickable() {
      return !this.isRemoved();
   }

   @Override
   protected double getDefaultGravity() {
      return 0.04;
   }

   @Override
   public void tick() {
      if (this.blockState.isAir()) {
         this.discard();
      } else {
         Block block = this.blockState.getBlock();
         this.time++;
         this.applyGravity();
         this.move(MoverType.SELF, this.getDeltaMovement());
         this.applyEffectsFromBlocks();
         this.handlePortal();
         if (this.level() instanceof ServerLevel serverLevel && (this.isAlive() || this.forceTickAfterTeleportToDuplicate)) {
            BlockPos pos = this.blockPosition();
            boolean isConcrete = this.blockState.getBlock() instanceof ConcretePowderBlock;
            boolean isStuckInWater = isConcrete && this.level().getFluidState(pos).is(FluidTags.WATER);
            double moveVec = this.getDeltaMovement().lengthSqr();
            if (isConcrete && moveVec > 1.0) {
               BlockHitResult clip = this.level()
                  .clip(new ClipContext(new Vec3(this.xo, this.yo, this.zo), this.position(), ClipContext.Block.COLLIDER, ClipContext.Fluid.SOURCE_ONLY, this));
               if (clip.getType() != HitResult.Type.MISS && this.level().getFluidState(clip.getBlockPos()).is(FluidTags.WATER)) {
                  pos = clip.getBlockPos();
                  isStuckInWater = true;
               }
            }

            if (!this.onGround() && !isStuckInWater) {
               if (this.time > 100 && (pos.getY() <= this.level().getMinY() || pos.getY() > this.level().getMaxY()) || this.time > 600) {
                  if (this.dropItem && serverLevel.getGameRules().get(GameRules.ENTITY_DROPS)) {
                     this.spawnAtLocation(serverLevel, block);
                  }

                  this.discard();
               }
            } else {
               BlockState currentState = this.level().getBlockState(pos);
               this.setDeltaMovement(this.getDeltaMovement().multiply(0.7, -0.5, 0.7));
               if (!currentState.is(Blocks.MOVING_PISTON)) {
                  if (!this.cancelDrop) {
                     boolean mayReplace = currentState.canBeReplaced(
                        new DirectionalPlaceContext(this.level(), pos, Direction.DOWN, ItemStack.EMPTY, Direction.UP)
                     );
                     boolean wouldContinueFalling = FallingBlock.isFree(this.level().getBlockState(pos.below())) && (!isConcrete || !isStuckInWater);
                     boolean wouldSurvive = this.blockState.canSurvive(this.level(), pos) && !wouldContinueFalling;
                     if (mayReplace && wouldSurvive) {
                        if (this.blockState.hasProperty(BlockStateProperties.WATERLOGGED) && this.level().getFluidState(pos).is(Fluids.WATER)) {
                           this.blockState = this.blockState.setValue(BlockStateProperties.WATERLOGGED, true);
                        }

                        if (this.level().setBlockAndUpdate(pos, this.blockState)) {
                           serverLevel.getChunkSource()
                              .chunkMap
                              .sendToTrackingPlayers(this, new ClientboundBlockUpdatePacket(pos, this.level().getBlockState(pos)));
                           this.discard();
                           if (block instanceof Fallable fallable) {
                              fallable.onLand(this.level(), pos, this.blockState, currentState, this);
                           }

                           if (this.blockData != null && this.blockState.hasBlockEntity()) {
                              BlockEntity blockEntity = this.level().getBlockEntity(pos);
                              if (blockEntity != null) {
                                 try (ProblemReporter.ScopedCollector reporter = new ProblemReporter.ScopedCollector(blockEntity.problemPath(), LOGGER)) {
                                    RegistryAccess registryAccess = this.level().registryAccess();
                                    TagValueOutput output = TagValueOutput.createWithContext(reporter, registryAccess);
                                    blockEntity.saveWithoutMetadata(output);
                                    CompoundTag merged = output.buildResult();
                                    this.blockData.forEach((name, tag) -> merged.put(name, tag.copy()));
                                    blockEntity.loadWithComponents(TagValueInput.create(reporter, registryAccess, merged));
                                 } catch (Exception e) {
                                    LOGGER.error("Failed to load block entity from falling block", e);
                                 }

                                 blockEntity.setChanged();
                              }
                           }
                        } else if (this.dropItem && serverLevel.getGameRules().get(GameRules.ENTITY_DROPS)) {
                           this.discard();
                           this.callOnBrokenAfterFall(block, pos);
                           this.spawnAtLocation(serverLevel, block);
                        }
                     } else {
                        this.discard();
                        if (this.dropItem && serverLevel.getGameRules().get(GameRules.ENTITY_DROPS)) {
                           this.callOnBrokenAfterFall(block, pos);
                           this.spawnAtLocation(serverLevel, block);
                        }
                     }
                  } else {
                     this.discard();
                     this.callOnBrokenAfterFall(block, pos);
                  }
               }
            }
         }

         this.setDeltaMovement(this.getDeltaMovement().scale(this.getAirDrag()));
         this.setRequiresPrecisePosition(this.horizontalCollision || this.verticalCollision);
      }
   }

   public void callOnBrokenAfterFall(final Block block, final BlockPos pos) {
      if (block instanceof Fallable fallable) {
         fallable.onBrokenAfterFall(this.level(), pos, this);
      }
   }

   @Override
   public boolean causeFallDamage(final double fallDistance, final float damageModifier, final DamageSource damageSource) {
      if (!this.hurtEntities) {
         return false;
      }

      int fallDistanceInt = Mth.ceil(fallDistance - 1.0);
      if (fallDistanceInt < 0) {
         return false;
      }

      Predicate<Entity> entitySelector = EntitySelector.NO_CREATIVE_OR_SPECTATOR.and(EntitySelector.LIVING_ENTITY_STILL_ALIVE);
      DamageSource actualDamageSource = this.blockState.getBlock() instanceof Fallable fallable
         ? fallable.getFallDamageSource(this)
         : this.damageSources().fallingBlock(this);
      float damage = Math.min(Mth.floor(fallDistanceInt * this.fallDamagePerDistance), this.fallDamageMax);
      this.level().getEntities(this, this.getBoundingBox(), entitySelector).forEach(entity -> entity.hurt(actualDamageSource, damage));
      boolean isAnvil = this.blockState.is(BlockTags.ANVIL);
      if (isAnvil && damage > 0.0F && this.random.nextFloat() < 0.05F + fallDistanceInt * 0.05F) {
         BlockState newBlockState = AnvilBlock.damage(this.blockState);
         if (newBlockState == null) {
            this.cancelDrop = true;
         } else {
            this.blockState = newBlockState;
         }
      }

      return false;
   }

   @Override
   protected void addAdditionalSaveData(final ValueOutput output) {
      output.store("BlockState", BlockState.CODEC, this.blockState);
      output.putInt("Time", this.time);
      output.putBoolean("DropItem", this.dropItem);
      output.putBoolean("HurtEntities", this.hurtEntities);
      output.putFloat("FallHurtAmount", this.fallDamagePerDistance);
      output.putInt("FallHurtMax", this.fallDamageMax);
      if (this.blockData != null) {
         output.store("TileEntityData", CompoundTag.CODEC, this.blockData);
      }

      output.putBoolean("CancelDrop", this.cancelDrop);
   }

   @Override
   protected void readAdditionalSaveData(final ValueInput input) {
      this.blockState = input.<BlockState>read("BlockState", BlockState.CODEC).orElse(DEFAULT_BLOCK_STATE);
      this.time = input.getIntOr("Time", 0);
      boolean defaultHurtEntities = this.blockState.is(BlockTags.ANVIL);
      this.hurtEntities = input.getBooleanOr("HurtEntities", defaultHurtEntities);
      this.fallDamagePerDistance = input.getFloatOr("FallHurtAmount", 0.0F);
      this.fallDamageMax = input.getIntOr("FallHurtMax", 40);
      this.dropItem = input.getBooleanOr("DropItem", true);
      this.blockData = input.<CompoundTag>read("TileEntityData", CompoundTag.CODEC).orElse(null);
      this.cancelDrop = input.getBooleanOr("CancelDrop", false);
   }

   public void setHurtsEntities(final float damagePerDistance, final int damageMax) {
      this.hurtEntities = true;
      this.fallDamagePerDistance = damagePerDistance;
      this.fallDamageMax = damageMax;
   }

   public void disableDrop() {
      this.cancelDrop = true;
   }

   @Override
   public boolean displayFireAnimation() {
      return false;
   }

   @Override
   public void fillCrashReportCategory(final CrashReportCategory category) {
      super.fillCrashReportCategory(category);
      category.setDetail("Immitating BlockState", this.blockState.toString());
   }

   public BlockState getBlockState() {
      return this.blockState;
   }

   @Override
   protected Component getTypeName() {
      return Component.translatable("entity.minecraft.falling_block_type", this.blockState.getBlock().getName());
   }

   @Override
   public Packet<ClientGamePacketListener> getAddEntityPacket(final ServerEntity serverEntity) {
      return new ClientboundAddEntityPacket(this, serverEntity, Block.getId(this.getBlockState()));
   }

   @Override
   public void recreateFromPacket(final ClientboundAddEntityPacket packet) {
      super.recreateFromPacket(packet);
      this.blockState = Block.stateById(packet.getData());
      this.blocksBuilding = true;
      double x = packet.getX();
      double y = packet.getY();
      double z = packet.getZ();
      this.setPos(x, y, z);
      this.setStartPos(this.blockPosition());
   }

   @Override
   public @Nullable Entity teleport(final TeleportTransition transition) {
      ResourceKey<Level> newDimension = transition.newLevel().dimension();
      ResourceKey<Level> oldDimension = this.level().dimension();
      boolean fromOrToEnd = (oldDimension == Level.END || newDimension == Level.END) && oldDimension != newDimension;
      Entity newEntity = super.teleport(transition);
      this.forceTickAfterTeleportToDuplicate = newEntity != null && fromOrToEnd;
      return newEntity;
   }

   @Override
   public MoveSimulationType getMoveSimulationType() {
      return MoveSimulationType.SERVER_AND_CLIENT;
   }
}
