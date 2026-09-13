package net.minecraft.server.level;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.CollisionGetter;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class PlayerSpawnFinder {
   private static final EntityDimensions PLAYER_DIMENSIONS = EntityType.PLAYER.getDimensions();
   private static final int ABSOLUTE_MAX_ATTEMPTS = 1024;
   private final ServerLevel level;
   private final BlockPos spawnSuggestion;
   private final int radius;
   private final int candidateCount;
   private final int coprime;
   private final int offset;
   private int nextCandidateIndex;
   private final CompletableFuture<Vec3> finishedFuture = new CompletableFuture<>();

   private PlayerSpawnFinder(ServerLevel p_427388_, BlockPos p_425434_, int p_425474_) {
      this.level = p_427388_;
      this.spawnSuggestion = p_425434_;
      this.radius = p_425474_;
      long i = p_425474_ * 2L + 1L;
      this.candidateCount = (int)Math.min(1024L, i * i);
      this.coprime = getCoprime(this.candidateCount);
      this.offset = RandomSource.create().nextInt(this.candidateCount);
   }

   public static CompletableFuture<Vec3> findSpawn(ServerLevel p_423721_, BlockPos p_427181_) {
      if (p_423721_.dimensionType().hasSkyLight() && p_423721_.getServer().getWorldData().getGameType() != GameType.ADVENTURE) {
         int i = Math.max(0, p_423721_.getGameRules().get(GameRules.RESPAWN_RADIUS));
         int j = Mth.floor(p_423721_.getWorldBorder().getDistanceToBorder(p_427181_.getX(), p_427181_.getZ()));
         if (j < i) {
            i = j;
         }

         if (j <= 1) {
            i = 1;
         }

         PlayerSpawnFinder playerspawnfinder = new PlayerSpawnFinder(p_423721_, p_427181_, i);
         playerspawnfinder.scheduleNext();
         return playerspawnfinder.finishedFuture;
      } else {
         return CompletableFuture.completedFuture(fixupSpawnHeight(p_423721_, p_427181_));
      }
   }

   private void scheduleNext() {
      int i = this.nextCandidateIndex++;
      if (i < this.candidateCount) {
         int j = (this.offset + this.coprime * i) % this.candidateCount;
         int k = j % (this.radius * 2 + 1);
         int l = j / (this.radius * 2 + 1);
         int i1 = this.spawnSuggestion.getX() + k - this.radius;
         int j1 = this.spawnSuggestion.getZ() + l - this.radius;
         this.scheduleCandidate(i1, j1, i, () -> {
            BlockPos blockpos = getOverworldRespawnPos(this.level, i1, j1);
            return blockpos != null && noCollisionNoLiquid(this.level, blockpos) ? Optional.of(Vec3.atBottomCenterOf(blockpos)) : Optional.empty();
         });
      } else {
         this.scheduleCandidate(
            this.spawnSuggestion.getX(), this.spawnSuggestion.getZ(), i, () -> Optional.of(fixupSpawnHeight(this.level, this.spawnSuggestion))
         );
      }
   }

   private static Vec3 fixupSpawnHeight(CollisionGetter p_422756_, BlockPos p_422886_) {
      BlockPos.MutableBlockPos blockpos$mutableblockpos = p_422886_.mutable();

      while (!noCollisionNoLiquid(p_422756_, blockpos$mutableblockpos) && blockpos$mutableblockpos.getY() < p_422756_.getMaxY()) {
         blockpos$mutableblockpos.move(Direction.UP);
      }

      blockpos$mutableblockpos.move(Direction.DOWN);

      while (noCollisionNoLiquid(p_422756_, blockpos$mutableblockpos) && blockpos$mutableblockpos.getY() > p_422756_.getMinY()) {
         blockpos$mutableblockpos.move(Direction.DOWN);
      }

      blockpos$mutableblockpos.move(Direction.UP);
      return Vec3.atBottomCenterOf(blockpos$mutableblockpos);
   }

   private static boolean noCollisionNoLiquid(CollisionGetter p_431646_, BlockPos p_423680_) {
      return p_431646_.noCollision(null, PLAYER_DIMENSIONS.makeBoundingBox(p_423680_.getBottomCenter()), true);
   }

   private static int getCoprime(int p_430552_) {
      return p_430552_ <= 16 ? p_430552_ - 1 : 17;
   }

   private void scheduleCandidate(int p_428569_, int p_426362_, int p_422626_, Supplier<Optional<Vec3>> p_424607_) {
      if (!this.finishedFuture.isDone()) {
         int i = SectionPos.blockToSectionCoord(p_428569_);
         int j = SectionPos.blockToSectionCoord(p_426362_);
         this.level.getChunkSource().addTicketAndLoadWithRadius(TicketType.SPAWN_SEARCH, new ChunkPos(i, j), 0).whenCompleteAsync((p_423970_, p_426971_) -> {
            if (p_426971_ == null) {
               try {
                  Optional<Vec3> optional = p_424607_.get();
                  if (optional.isPresent()) {
                     this.finishedFuture.complete(optional.get());
                  } else {
                     this.scheduleNext();
                  }
               } catch (Throwable throwable) {
                  p_426971_ = throwable;
               }
            }

            if (p_426971_ != null) {
               CrashReport crashreport = CrashReport.forThrowable(p_426971_, "Searching for spawn");
               CrashReportCategory crashreportcategory = crashreport.addCategory("Spawn Lookup");
               crashreportcategory.setDetail("Origin", this.spawnSuggestion::toString);
               crashreportcategory.setDetail("Radius", () -> Integer.toString(this.radius));
               crashreportcategory.setDetail("Candidate", () -> "[" + p_428569_ + "," + p_426362_ + "]");
               crashreportcategory.setDetail("Progress", () -> p_422626_ + " out of " + this.candidateCount);
               this.finishedFuture.completeExceptionally(new ReportedException(crashreport));
            }
         }, this.level.getServer());
      }
   }

   protected static @Nullable BlockPos getOverworldRespawnPos(ServerLevel p_425234_, int p_426463_, int p_427199_) {
      boolean flag = p_425234_.dimensionType().hasCeiling();
      LevelChunk levelchunk = p_425234_.getChunk(SectionPos.blockToSectionCoord(p_426463_), SectionPos.blockToSectionCoord(p_427199_));
      int i = flag
         ? p_425234_.getChunkSource().getGenerator().getSpawnHeight(p_425234_)
         : levelchunk.getHeight(Heightmap.Types.MOTION_BLOCKING, p_426463_ & 15, p_427199_ & 15);
      if (i < p_425234_.getMinY()) {
         return null;
      }

      int j = levelchunk.getHeight(Heightmap.Types.WORLD_SURFACE, p_426463_ & 15, p_427199_ & 15);
      if (j <= i && j > levelchunk.getHeight(Heightmap.Types.OCEAN_FLOOR, p_426463_ & 15, p_427199_ & 15)) {
         return null;
      }

      BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();

      for (int k = i + 1; k >= p_425234_.getMinY(); k--) {
         blockpos$mutableblockpos.set(p_426463_, k, p_427199_);
         BlockState blockstate = p_425234_.getBlockState(blockpos$mutableblockpos);
         if (!blockstate.getFluidState().isEmpty()) {
            break;
         }

         if (Block.isFaceFull(blockstate.getCollisionShape(p_425234_, blockpos$mutableblockpos), Direction.UP)) {
            return blockpos$mutableblockpos.above().immutable();
         }
      }

      return null;
   }

   public static @Nullable BlockPos getSpawnPosInChunk(ServerLevel p_423884_, ChunkPos p_427355_) {
      if (SharedConstants.debugVoidTerrain(p_427355_)) {
         return null;
      }

      for (int i = p_427355_.getMinBlockX(); i <= p_427355_.getMaxBlockX(); i++) {
         for (int j = p_427355_.getMinBlockZ(); j <= p_427355_.getMaxBlockZ(); j++) {
            BlockPos blockpos = getOverworldRespawnPos(p_423884_, i, j);
            if (blockpos != null) {
               return blockpos;
            }
         }
      }

      return null;
   }
}
