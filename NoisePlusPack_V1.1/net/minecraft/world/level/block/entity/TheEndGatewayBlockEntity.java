package net.minecraft.world.level.block.entity;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.features.EndFeatures;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.EndGatewayConfiguration;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class TheEndGatewayBlockEntity extends TheEndPortalBlockEntity {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final int SPAWN_TIME = 200;
   private static final int COOLDOWN_TIME = 40;
   private static final int ATTENTION_INTERVAL = 2400;
   private static final int EVENT_COOLDOWN = 1;
   private static final int GATEWAY_HEIGHT_ABOVE_SURFACE = 10;
   private static final long DEFAULT_AGE = 0L;
   private static final boolean DEFAULT_EXACT_TELEPORT = false;
   private long age = 0L;
   private int teleportCooldown;
   private @Nullable BlockPos exitPortal;
   private boolean exactTeleport = false;

   public TheEndGatewayBlockEntity(BlockPos p_155813_, BlockState p_155814_) {
      super(BlockEntityType.END_GATEWAY, p_155813_, p_155814_);
   }

   @Override
   protected void saveAdditional(ValueOutput p_407878_) {
      super.saveAdditional(p_407878_);
      p_407878_.putLong("Age", this.age);
      p_407878_.storeNullable("exit_portal", BlockPos.CODEC, this.exitPortal);
      if (this.exactTeleport) {
         p_407878_.putBoolean("ExactTeleport", true);
      }
   }

   @Override
   protected void loadAdditional(ValueInput p_408324_) {
      super.loadAdditional(p_408324_);
      this.age = p_408324_.getLongOr("Age", 0L);
      this.exitPortal = p_408324_.<BlockPos>read("exit_portal", BlockPos.CODEC).filter(Level::isInSpawnableBounds).orElse(null);
      this.exactTeleport = p_408324_.getBooleanOr("ExactTeleport", false);
   }

   public static void beamAnimationTick(Level p_155835_, BlockPos p_155836_, BlockState p_155837_, TheEndGatewayBlockEntity p_155838_) {
      p_155838_.age++;
      if (p_155838_.isCoolingDown()) {
         p_155838_.teleportCooldown--;
      }
   }

   public static void portalTick(Level p_344808_, BlockPos p_342267_, BlockState p_344200_, TheEndGatewayBlockEntity p_343419_) {
      boolean flag = p_343419_.isSpawning();
      boolean flag1 = p_343419_.isCoolingDown();
      p_343419_.age++;
      if (flag1) {
         p_343419_.teleportCooldown--;
      } else if (p_343419_.age % 2400L == 0L) {
         triggerCooldown(p_344808_, p_342267_, p_344200_, p_343419_);
      }

      if (flag != p_343419_.isSpawning() || flag1 != p_343419_.isCoolingDown()) {
         setChanged(p_344808_, p_342267_, p_344200_);
      }
   }

   public boolean isSpawning() {
      return this.age < 200L;
   }

   public boolean isCoolingDown() {
      return this.teleportCooldown > 0;
   }

   public float getSpawnPercent(float p_59934_) {
      return Mth.clamp(((float)this.age + p_59934_) / 200.0F, 0.0F, 1.0F);
   }

   public float getCooldownPercent(float p_59968_) {
      return 1.0F - Mth.clamp((this.teleportCooldown - p_59968_) / 40.0F, 0.0F, 1.0F);
   }

   public ClientboundBlockEntityDataPacket getUpdatePacket() {
      return ClientboundBlockEntityDataPacket.create(this);
   }

   @Override
   public CompoundTag getUpdateTag(HolderLookup.Provider p_332673_) {
      return this.saveCustomOnly(p_332673_);
   }

   public static void triggerCooldown(Level p_155850_, BlockPos p_155851_, BlockState p_155852_, TheEndGatewayBlockEntity p_155853_) {
      if (!p_155850_.isClientSide()) {
         p_155853_.teleportCooldown = 40;
         p_155850_.blockEvent(p_155851_, p_155852_.getBlock(), 1, 0);
         setChanged(p_155850_, p_155851_, p_155852_);
      }
   }

   @Override
   public boolean triggerEvent(int p_59963_, int p_59964_) {
      if (p_59963_ == 1) {
         this.teleportCooldown = 40;
         return true;
      } else {
         return super.triggerEvent(p_59963_, p_59964_);
      }
   }

   public @Nullable Vec3 getPortalPosition(ServerLevel p_342945_, BlockPos p_345486_) {
      if (this.exitPortal == null && p_342945_.dimension() == Level.END) {
         BlockPos blockpos = findOrCreateValidTeleportPos(p_342945_, p_345486_);
         blockpos = blockpos.above(10);
         LOGGER.debug("Creating portal at {}", blockpos);
         spawnGatewayPortal(p_342945_, blockpos, EndGatewayConfiguration.knownExit(p_345486_, false));
         this.setExitPosition(blockpos, this.exactTeleport);
      }

      if (this.exitPortal != null) {
         BlockPos blockpos1 = this.exactTeleport ? this.exitPortal : findExitPosition(p_342945_, this.exitPortal);
         return blockpos1.getBottomCenter();
      } else {
         return null;
      }
   }

   private static BlockPos findExitPosition(Level p_155826_, BlockPos p_155827_) {
      BlockPos blockpos = findTallestBlock(p_155826_, p_155827_.offset(0, 2, 0), 5, false);
      LOGGER.debug("Best exit position for portal at {} is {}", p_155827_, blockpos);
      return blockpos.above();
   }

   private static BlockPos findOrCreateValidTeleportPos(ServerLevel p_155819_, BlockPos p_155820_) {
      Vec3 vec3 = findExitPortalXZPosTentative(p_155819_, p_155820_);
      LevelChunk levelchunk = getChunk(p_155819_, vec3);
      BlockPos blockpos = findValidSpawnInChunk(levelchunk);
      if (blockpos == null) {
         BlockPos blockpos1 = BlockPos.containing(vec3.x + 0.5, 75.0, vec3.z + 0.5);
         LOGGER.debug("Failed to find a suitable block to teleport to, spawning an island on {}", blockpos1);
         p_155819_.registryAccess()
            .lookup(Registries.CONFIGURED_FEATURE)
            .flatMap(p_360496_ -> p_360496_.get(EndFeatures.END_ISLAND))
            .ifPresent(
               p_256040_ -> p_256040_.value().place(p_155819_, p_155819_.getChunkSource().getGenerator(), RandomSource.create(blockpos1.asLong()), blockpos1)
            );
         blockpos = blockpos1;
      } else {
         LOGGER.debug("Found suitable block to teleport to: {}", blockpos);
      }

      return findTallestBlock(p_155819_, blockpos, 16, true);
   }

   private static Vec3 findExitPortalXZPosTentative(ServerLevel p_155842_, BlockPos p_155843_) {
      Vec3 vec3 = new Vec3(p_155843_.getX(), 0.0, p_155843_.getZ()).normalize();
      int i = 1024;
      Vec3 vec31 = vec3.scale(1024.0);

      for (int j = 16; !isChunkEmpty(p_155842_, vec31) && j-- > 0; vec31 = vec31.add(vec3.scale(-16.0))) {
         LOGGER.debug("Skipping backwards past nonempty chunk at {}", vec31);
      }

      for (int k = 16; isChunkEmpty(p_155842_, vec31) && k-- > 0; vec31 = vec31.add(vec3.scale(16.0))) {
         LOGGER.debug("Skipping forward past empty chunk at {}", vec31);
      }

      LOGGER.debug("Found chunk at {}", vec31);
      return vec31;
   }

   private static boolean isChunkEmpty(ServerLevel p_155816_, Vec3 p_155817_) {
      return getChunk(p_155816_, p_155817_).getHighestFilledSectionIndex() == -1;
   }

   private static BlockPos findTallestBlock(BlockGetter p_59943_, BlockPos p_59944_, int p_59945_, boolean p_59946_) {
      BlockPos blockpos = null;

      for (int i = -p_59945_; i <= p_59945_; i++) {
         for (int j = -p_59945_; j <= p_59945_; j++) {
            if (i != 0 || j != 0 || p_59946_) {
               for (int k = p_59943_.getMaxY(); k > (blockpos == null ? p_59943_.getMinY() : blockpos.getY()); k--) {
                  BlockPos blockpos1 = new BlockPos(p_59944_.getX() + i, k, p_59944_.getZ() + j);
                  BlockState blockstate = p_59943_.getBlockState(blockpos1);
                  if (blockstate.isCollisionShapeFullBlock(p_59943_, blockpos1) && (p_59946_ || !blockstate.is(Blocks.BEDROCK))) {
                     blockpos = blockpos1;
                     break;
                  }
               }
            }
         }
      }

      return blockpos == null ? p_59944_ : blockpos;
   }

   private static LevelChunk getChunk(Level p_59948_, Vec3 p_59949_) {
      return p_59948_.getChunk(Mth.floor(p_59949_.x / 16.0), Mth.floor(p_59949_.z / 16.0));
   }

   private static @Nullable BlockPos findValidSpawnInChunk(LevelChunk p_59954_) {
      ChunkPos chunkpos = p_59954_.getPos();
      BlockPos blockpos = new BlockPos(chunkpos.getMinBlockX(), 30, chunkpos.getMinBlockZ());
      int i = p_59954_.getHighestSectionPosition() + 16 - 1;
      BlockPos blockpos1 = new BlockPos(chunkpos.getMaxBlockX(), i, chunkpos.getMaxBlockZ());
      BlockPos blockpos2 = null;
      double d0 = 0.0;

      for (BlockPos blockpos3 : BlockPos.betweenClosed(blockpos, blockpos1)) {
         BlockState blockstate = p_59954_.getBlockState(blockpos3);
         BlockPos blockpos4 = blockpos3.above();
         BlockPos blockpos5 = blockpos3.above(2);
         if (blockstate.is(Blocks.END_STONE)
            && !p_59954_.getBlockState(blockpos4).isCollisionShapeFullBlock(p_59954_, blockpos4)
            && !p_59954_.getBlockState(blockpos5).isCollisionShapeFullBlock(p_59954_, blockpos5)) {
            double d1 = blockpos3.distToCenterSqr(0.0, 0.0, 0.0);
            if (blockpos2 == null || d1 < d0) {
               blockpos2 = blockpos3;
               d0 = d1;
            }
         }
      }

      return blockpos2;
   }

   private static void spawnGatewayPortal(ServerLevel p_155822_, BlockPos p_155823_, EndGatewayConfiguration p_155824_) {
      Feature.END_GATEWAY.place(p_155824_, p_155822_, p_155822_.getChunkSource().getGenerator(), RandomSource.create(), p_155823_);
   }

   @Override
   public boolean shouldRenderFace(Direction p_59959_) {
      return Block.shouldRenderFace(this.getBlockState(), this.level.getBlockState(this.getBlockPos().relative(p_59959_)), p_59959_);
   }

   public int getParticleAmount() {
      int i = 0;

      for (Direction direction : Direction.values()) {
         i += this.shouldRenderFace(direction) ? 1 : 0;
      }

      return i;
   }

   public void setExitPosition(BlockPos p_59956_, boolean p_59957_) {
      this.exactTeleport = p_59957_;
      this.exitPortal = p_59956_;
      this.setChanged();
   }
}
