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

   public FallingBlockEntity(EntityType<? extends FallingBlockEntity> p_31950_, Level p_31951_) {
      super(p_31950_, p_31951_);
   }

   private FallingBlockEntity(Level p_31953_, double p_31954_, double p_31955_, double p_31956_, BlockState p_31957_) {
      this(EntityType.FALLING_BLOCK, p_31953_);
      this.blockState = p_31957_;
      this.blocksBuilding = true;
      this.setPos(p_31954_, p_31955_, p_31956_);
      this.setDeltaMovement(Vec3.ZERO);
      this.xo = p_31954_;
      this.yo = p_31955_;
      this.zo = p_31956_;
      this.setStartPos(this.blockPosition());
   }

   public static FallingBlockEntity fall(Level p_201972_, BlockPos p_201973_, BlockState p_201974_) {
      FallingBlockEntity fallingblockentity = new FallingBlockEntity(
         p_201972_,
         p_201973_.getX() + 0.5,
         p_201973_.getY(),
         p_201973_.getZ() + 0.5,
         p_201974_.hasProperty(BlockStateProperties.WATERLOGGED) ? p_201974_.setValue(BlockStateProperties.WATERLOGGED, false) : p_201974_
      );
      p_201972_.setBlock(p_201973_, p_201974_.getFluidState().createLegacyBlock(), 3);
      p_201972_.addFreshEntity(fallingblockentity);
      return fallingblockentity;
   }

   @Override
   public boolean isAttackable() {
      return false;
   }

   @Override
   public final boolean hurtServer(ServerLevel p_369142_, DamageSource p_361302_, float p_361003_) {
      if (!this.isInvulnerableToBase(p_361302_)) {
         this.markHurt();
      }

      return false;
   }

   public void setStartPos(BlockPos p_31960_) {
      this.entityData.set(DATA_START_POS, p_31960_);
   }

   public BlockPos getStartPos() {
      return this.entityData.get(DATA_START_POS);
   }

   @Override
   protected Entity.MovementEmission getMovementEmission() {
      return Entity.MovementEmission.NONE;
   }

   @Override
   protected void defineSynchedData(SynchedEntityData.Builder p_329911_) {
      p_329911_.define(DATA_START_POS, BlockPos.ZERO);
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
         if (this.level() instanceof ServerLevel serverlevel && (this.isAlive() || this.forceTickAfterTeleportToDuplicate)) {
            BlockPos blockpos = this.blockPosition();
            boolean flag = this.blockState.getBlock() instanceof ConcretePowderBlock;
            boolean flag1 = flag && this.level().getFluidState(blockpos).is(FluidTags.WATER);
            double d0 = this.getDeltaMovement().lengthSqr();
            if (flag && d0 > 1.0) {
               BlockHitResult blockhitresult = this.level()
                  .clip(new ClipContext(new Vec3(this.xo, this.yo, this.zo), this.position(), ClipContext.Block.COLLIDER, ClipContext.Fluid.SOURCE_ONLY, this));
               if (blockhitresult.getType() != HitResult.Type.MISS && this.level().getFluidState(blockhitresult.getBlockPos()).is(FluidTags.WATER)) {
                  blockpos = blockhitresult.getBlockPos();
                  flag1 = true;
               }
            }

            if (!this.onGround() && !flag1) {
               if (this.time > 100 && (blockpos.getY() <= this.level().getMinY() || blockpos.getY() > this.level().getMaxY()) || this.time > 600) {
                  if (this.dropItem && serverlevel.getGameRules().get(GameRules.ENTITY_DROPS)) {
                     this.spawnAtLocation(serverlevel, block);
                  }

                  this.discard();
               }
            } else {
               BlockState blockstate = this.level().getBlockState(blockpos);
               this.setDeltaMovement(this.getDeltaMovement().multiply(0.7, -0.5, 0.7));
               if (!blockstate.is(Blocks.MOVING_PISTON)) {
                  if (this.cancelDrop) {
                     this.discard();
                     this.callOnBrokenAfterFall(block, blockpos);
                  } else {
                     boolean flag2 = blockstate.canBeReplaced(
                        new DirectionalPlaceContext(this.level(), blockpos, Direction.DOWN, ItemStack.EMPTY, Direction.UP)
                     );
                     boolean flag3 = FallingBlock.isFree(this.level().getBlockState(blockpos.below())) && (!flag || !flag1);
                     boolean flag4 = this.blockState.canSurvive(this.level(), blockpos) && !flag3;
                     if (flag2 && flag4) {
                        if (this.blockState.hasProperty(BlockStateProperties.WATERLOGGED) && this.level().getFluidState(blockpos).getType() == Fluids.WATER) {
                           this.blockState = this.blockState.setValue(BlockStateProperties.WATERLOGGED, true);
                        }

                        if (this.level().setBlock(blockpos, this.blockState, 3)) {
                           serverlevel.getChunkSource()
                              .chunkMap
                              .sendToTrackingPlayers(this, new ClientboundBlockUpdatePacket(blockpos, this.level().getBlockState(blockpos)));
                           this.discard();
                           if (block instanceof Fallable fallable) {
                              fallable.onLand(this.level(), blockpos, this.blockState, blockstate, this);
                           }

                           if (this.blockData != null && this.blockState.hasBlockEntity()) {
                              BlockEntity blockentity = this.level().getBlockEntity(blockpos);
                              if (blockentity != null) {
                                 try (ProblemReporter.ScopedCollector problemreporter$scopedcollector = new ProblemReporter.ScopedCollector(
                                       blockentity.problemPath(), LOGGER
                                    )) {
                                    RegistryAccess registryaccess = this.level().registryAccess();
                                    TagValueOutput tagvalueoutput = TagValueOutput.createWithContext(problemreporter$scopedcollector, registryaccess);
                                    blockentity.saveWithoutMetadata(tagvalueoutput);
                                    CompoundTag compoundtag = tagvalueoutput.buildResult();
                                    this.blockData.forEach((p_390676_, p_390677_) -> compoundtag.put(p_390676_, p_390677_.copy()));
                                    blockentity.loadWithComponents(TagValueInput.create(problemreporter$scopedcollector, registryaccess, compoundtag));
                                 } catch (Exception exception) {
                                    LOGGER.error("Failed to load block entity from falling block", exception);
                                 }

                                 blockentity.setChanged();
                              }
                           }
                        } else if (this.dropItem && serverlevel.getGameRules().get(GameRules.ENTITY_DROPS)) {
                           this.discard();
                           this.callOnBrokenAfterFall(block, blockpos);
                           this.spawnAtLocation(serverlevel, block);
                        }
                     } else {
                        this.discard();
                        if (this.dropItem && serverlevel.getGameRules().get(GameRules.ENTITY_DROPS)) {
                           this.callOnBrokenAfterFall(block, blockpos);
                           this.spawnAtLocation(serverlevel, block);
                        }
                     }
                  }
               }
            }
         }

         this.setDeltaMovement(this.getDeltaMovement().scale(0.98));
      }
   }

   public void callOnBrokenAfterFall(Block p_149651_, BlockPos p_149652_) {
      if (p_149651_ instanceof Fallable) {
         ((Fallable)p_149651_).onBrokenAfterFall(this.level(), p_149652_, this);
      }
   }

   @Override
   public boolean causeFallDamage(double p_395056_, float p_149643_, DamageSource p_149645_) {
      if (!this.hurtEntities) {
         return false;
      }

      int i = Mth.ceil(p_395056_ - 1.0);
      if (i < 0) {
         return false;
      }

      Predicate<Entity> predicate = EntitySelector.NO_CREATIVE_OR_SPECTATOR.and(EntitySelector.LIVING_ENTITY_STILL_ALIVE);
      DamageSource damagesource = this.blockState.getBlock() instanceof Fallable fallable
         ? fallable.getFallDamageSource(this)
         : this.damageSources().fallingBlock(this);
      float f = Math.min(Mth.floor(i * this.fallDamagePerDistance), this.fallDamageMax);
      this.level().getEntities(this, this.getBoundingBox(), predicate).forEach(p_359237_ -> p_359237_.hurt(damagesource, f));
      boolean flag = this.blockState.is(BlockTags.ANVIL);
      if (flag && f > 0.0F && this.random.nextFloat() < 0.05F + i * 0.05F) {
         BlockState blockstate = AnvilBlock.damage(this.blockState);
         if (blockstate == null) {
            this.cancelDrop = true;
         } else {
            this.blockState = blockstate;
         }
      }

      return false;
   }

   @Override
   protected void addAdditionalSaveData(ValueOutput p_409478_) {
      p_409478_.store("BlockState", BlockState.CODEC, this.blockState);
      p_409478_.putInt("Time", this.time);
      p_409478_.putBoolean("DropItem", this.dropItem);
      p_409478_.putBoolean("HurtEntities", this.hurtEntities);
      p_409478_.putFloat("FallHurtAmount", this.fallDamagePerDistance);
      p_409478_.putInt("FallHurtMax", this.fallDamageMax);
      if (this.blockData != null) {
         p_409478_.store("TileEntityData", CompoundTag.CODEC, this.blockData);
      }

      p_409478_.putBoolean("CancelDrop", this.cancelDrop);
   }

   @Override
   protected void readAdditionalSaveData(ValueInput p_406690_) {
      this.blockState = p_406690_.<BlockState>read("BlockState", BlockState.CODEC).orElse(DEFAULT_BLOCK_STATE);
      this.time = p_406690_.getIntOr("Time", 0);
      boolean flag = this.blockState.is(BlockTags.ANVIL);
      this.hurtEntities = p_406690_.getBooleanOr("HurtEntities", flag);
      this.fallDamagePerDistance = p_406690_.getFloatOr("FallHurtAmount", 0.0F);
      this.fallDamageMax = p_406690_.getIntOr("FallHurtMax", 40);
      this.dropItem = p_406690_.getBooleanOr("DropItem", true);
      this.blockData = p_406690_.<CompoundTag>read("TileEntityData", CompoundTag.CODEC).orElse(null);
      this.cancelDrop = p_406690_.getBooleanOr("CancelDrop", false);
   }

   public void setHurtsEntities(float p_149657_, int p_149658_) {
      this.hurtEntities = true;
      this.fallDamagePerDistance = p_149657_;
      this.fallDamageMax = p_149658_;
   }

   public void disableDrop() {
      this.cancelDrop = true;
   }

   @Override
   public boolean displayFireAnimation() {
      return false;
   }

   @Override
   public void fillCrashReportCategory(CrashReportCategory p_31962_) {
      super.fillCrashReportCategory(p_31962_);
      p_31962_.setDetail("Immitating BlockState", this.blockState.toString());
   }

   public BlockState getBlockState() {
      return this.blockState;
   }

   @Override
   protected Component getTypeName() {
      return Component.translatable("entity.minecraft.falling_block_type", this.blockState.getBlock().getName());
   }

   @Override
   public Packet<ClientGamePacketListener> getAddEntityPacket(ServerEntity p_342166_) {
      return new ClientboundAddEntityPacket(this, p_342166_, Block.getId(this.getBlockState()));
   }

   @Override
   public void recreateFromPacket(ClientboundAddEntityPacket p_149654_) {
      super.recreateFromPacket(p_149654_);
      this.blockState = Block.stateById(p_149654_.getData());
      this.blocksBuilding = true;
      double d0 = p_149654_.getX();
      double d1 = p_149654_.getY();
      double d2 = p_149654_.getZ();
      this.setPos(d0, d1, d2);
      this.setStartPos(this.blockPosition());
   }

   @Override
   public @Nullable Entity teleport(TeleportTransition p_367033_) {
      ResourceKey<Level> resourcekey = p_367033_.newLevel().dimension();
      ResourceKey<Level> resourcekey1 = this.level().dimension();
      boolean flag = (resourcekey1 == Level.END || resourcekey == Level.END) && resourcekey1 != resourcekey;
      Entity entity = super.teleport(p_367033_);
      this.forceTickAfterTeleportToDuplicate = entity != null && flag;
      return entity;
   }
}
