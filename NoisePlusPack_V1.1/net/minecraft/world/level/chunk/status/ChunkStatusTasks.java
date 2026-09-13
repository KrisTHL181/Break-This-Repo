package net.minecraft.world.level.chunk.status;

import com.mojang.logging.LogUtils;
import java.util.EnumSet;
import java.util.concurrent.CompletableFuture;
import net.minecraft.SharedConstants;
import net.minecraft.server.level.GenerationChunkHolder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ThreadedLevelLightEngine;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.ProblemReporter;
import net.minecraft.util.StaticCache2D;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ImposterProtoChunk;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.levelgen.BelowZeroRetrogen;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.ValueInput;
import org.slf4j.Logger;

public class ChunkStatusTasks {
   private static final Logger LOGGER = LogUtils.getLogger();

   private static boolean isLighted(ChunkAccess p_332575_) {
      return p_332575_.getPersistedStatus().isOrAfter(ChunkStatus.LIGHT) && p_332575_.isLightCorrect();
   }

   static CompletableFuture<ChunkAccess> passThrough(
      WorldGenContext p_342543_, ChunkStep p_342704_, StaticCache2D<GenerationChunkHolder> p_343141_, ChunkAccess p_342339_
   ) {
      return CompletableFuture.completedFuture(p_342339_);
   }

   static CompletableFuture<ChunkAccess> generateStructureStarts(
      WorldGenContext p_333948_, ChunkStep p_345432_, StaticCache2D<GenerationChunkHolder> p_344447_, ChunkAccess p_332160_
   ) {
      ServerLevel serverlevel = p_333948_.level();
      if (serverlevel.getServer().getWorldData().worldGenOptions().generateStructures()) {
         p_333948_.generator()
            .createStructures(
               serverlevel.registryAccess(),
               serverlevel.getChunkSource().getGeneratorState(),
               serverlevel.structureManager(),
               p_332160_,
               p_333948_.structureManager(),
               serverlevel.dimension()
            );
      }

      serverlevel.onStructureStartsAvailable(p_332160_);
      return CompletableFuture.completedFuture(p_332160_);
   }

   static CompletableFuture<ChunkAccess> loadStructureStarts(
      WorldGenContext p_330330_, ChunkStep p_342490_, StaticCache2D<GenerationChunkHolder> p_344800_, ChunkAccess p_335780_
   ) {
      p_330330_.level().onStructureStartsAvailable(p_335780_);
      return CompletableFuture.completedFuture(p_335780_);
   }

   static CompletableFuture<ChunkAccess> generateStructureReferences(
      WorldGenContext p_334657_, ChunkStep p_342928_, StaticCache2D<GenerationChunkHolder> p_343099_, ChunkAccess p_335107_
   ) {
      ServerLevel serverlevel = p_334657_.level();
      WorldGenRegion worldgenregion = new WorldGenRegion(serverlevel, p_343099_, p_342928_, p_335107_);
      p_334657_.generator().createReferences(worldgenregion, serverlevel.structureManager().forWorldGenRegion(worldgenregion), p_335107_);
      return CompletableFuture.completedFuture(p_335107_);
   }

   static CompletableFuture<ChunkAccess> generateBiomes(
      WorldGenContext p_334080_, ChunkStep p_342859_, StaticCache2D<GenerationChunkHolder> p_342349_, ChunkAccess p_329246_
   ) {
      ServerLevel serverlevel = p_334080_.level();
      WorldGenRegion worldgenregion = new WorldGenRegion(serverlevel, p_342349_, p_342859_, p_329246_);
      return p_334080_.generator()
         .createBiomes(
            serverlevel.getChunkSource().randomState(), Blender.of(worldgenregion), serverlevel.structureManager().forWorldGenRegion(worldgenregion), p_329246_
         );
   }

   static CompletableFuture<ChunkAccess> generateNoise(
      WorldGenContext p_336010_, ChunkStep p_343333_, StaticCache2D<GenerationChunkHolder> p_343063_, ChunkAccess p_331391_
   ) {
      ServerLevel serverlevel = p_336010_.level();
      WorldGenRegion worldgenregion = new WorldGenRegion(serverlevel, p_343063_, p_343333_, p_331391_);
      return p_336010_.generator()
         .fillFromNoise(
            Blender.of(worldgenregion), serverlevel.getChunkSource().randomState(), serverlevel.structureManager().forWorldGenRegion(worldgenregion), p_331391_
         )
         .thenApply(p_328030_ -> {
            if (p_328030_ instanceof ProtoChunk protochunk) {
               BelowZeroRetrogen belowzeroretrogen = protochunk.getBelowZeroRetrogen();
               if (belowzeroretrogen != null) {
                  BelowZeroRetrogen.replaceOldBedrock(protochunk);
                  if (belowzeroretrogen.hasBedrockHoles()) {
                     belowzeroretrogen.applyBedrockMask(protochunk);
                  }
               }
            }

            return (ChunkAccess)p_328030_;
         });
   }

   static CompletableFuture<ChunkAccess> generateSurface(
      WorldGenContext p_331242_, ChunkStep p_345412_, StaticCache2D<GenerationChunkHolder> p_345033_, ChunkAccess p_329153_
   ) {
      ServerLevel serverlevel = p_331242_.level();
      WorldGenRegion worldgenregion = new WorldGenRegion(serverlevel, p_345033_, p_345412_, p_329153_);
      p_331242_.generator()
         .buildSurface(worldgenregion, serverlevel.structureManager().forWorldGenRegion(worldgenregion), serverlevel.getChunkSource().randomState(), p_329153_);
      return CompletableFuture.completedFuture(p_329153_);
   }

   static CompletableFuture<ChunkAccess> generateCarvers(
      WorldGenContext p_334842_, ChunkStep p_345337_, StaticCache2D<GenerationChunkHolder> p_343660_, ChunkAccess p_334473_
   ) {
      ServerLevel serverlevel = p_334842_.level();
      WorldGenRegion worldgenregion = new WorldGenRegion(serverlevel, p_343660_, p_345337_, p_334473_);
      if (p_334473_ instanceof ProtoChunk protochunk) {
         Blender.addAroundOldChunksCarvingMaskFilter(worldgenregion, protochunk);
      }

      p_334842_.generator()
         .applyCarvers(
            worldgenregion,
            serverlevel.getSeed(),
            serverlevel.getChunkSource().randomState(),
            serverlevel.getBiomeManager(),
            serverlevel.structureManager().forWorldGenRegion(worldgenregion),
            p_334473_
         );
      return CompletableFuture.completedFuture(p_334473_);
   }

   static CompletableFuture<ChunkAccess> generateFeatures(
      WorldGenContext p_330189_, ChunkStep p_344410_, StaticCache2D<GenerationChunkHolder> p_344248_, ChunkAccess p_332579_
   ) {
      ServerLevel serverlevel = p_330189_.level();
      Heightmap.primeHeightmaps(
         p_332579_,
         EnumSet.of(Heightmap.Types.MOTION_BLOCKING, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Heightmap.Types.OCEAN_FLOOR, Heightmap.Types.WORLD_SURFACE)
      );
      WorldGenRegion worldgenregion = new WorldGenRegion(serverlevel, p_344248_, p_344410_, p_332579_);
      if (!SharedConstants.DEBUG_DISABLE_FEATURES) {
         p_330189_.generator().applyBiomeDecoration(worldgenregion, p_332579_, serverlevel.structureManager().forWorldGenRegion(worldgenregion));
      }

      Blender.generateBorderTicks(worldgenregion, p_332579_);
      return CompletableFuture.completedFuture(p_332579_);
   }

   static CompletableFuture<ChunkAccess> initializeLight(
      WorldGenContext p_344706_, ChunkStep p_344577_, StaticCache2D<GenerationChunkHolder> p_344841_, ChunkAccess p_334426_
   ) {
      ThreadedLevelLightEngine threadedlevellightengine = p_344706_.lightEngine();
      p_334426_.initializeLightSources();
      ((ProtoChunk)p_334426_).setLightEngine(threadedlevellightengine);
      boolean flag = isLighted(p_334426_);
      return threadedlevellightengine.initializeLight(p_334426_, flag);
   }

   static CompletableFuture<ChunkAccess> light(
      WorldGenContext p_342217_, ChunkStep p_343464_, StaticCache2D<GenerationChunkHolder> p_342591_, ChunkAccess p_342577_
   ) {
      boolean flag = isLighted(p_342577_);
      return p_342217_.lightEngine().lightChunk(p_342577_, flag);
   }

   static CompletableFuture<ChunkAccess> generateSpawn(
      WorldGenContext p_329644_, ChunkStep p_343242_, StaticCache2D<GenerationChunkHolder> p_344209_, ChunkAccess p_329794_
   ) {
      if (!p_329794_.isUpgrading()) {
         p_329644_.generator().spawnOriginalMobs(new WorldGenRegion(p_329644_.level(), p_344209_, p_343242_, p_329794_));
      }

      return CompletableFuture.completedFuture(p_329794_);
   }

   static CompletableFuture<ChunkAccess> full(
      WorldGenContext p_342042_, ChunkStep p_345156_, StaticCache2D<GenerationChunkHolder> p_344754_, ChunkAccess p_342195_
   ) {
      ChunkPos chunkpos = p_342195_.getPos();
      GenerationChunkHolder generationchunkholder = p_344754_.get(chunkpos.x, chunkpos.z);
      return CompletableFuture.supplyAsync(
         () -> {
            ProtoChunk protochunk = (ProtoChunk)p_342195_;
            ServerLevel serverlevel = p_342042_.level();
            LevelChunk levelchunk;
            if (protochunk instanceof ImposterProtoChunk imposterprotochunk) {
               levelchunk = imposterprotochunk.getWrapped();
            } else {
               levelchunk = new LevelChunk(
                  serverlevel,
                  protochunk,
                  p_405757_ -> {
                     try (ProblemReporter.ScopedCollector problemreporter$scopedcollector = new ProblemReporter.ScopedCollector(p_342195_.problemPath(), LOGGER)) {
                        postLoadProtoChunk(
                           serverlevel, TagValueInput.create(problemreporter$scopedcollector, serverlevel.registryAccess(), protochunk.getEntities())
                        );
                     }
                  }
               );
               generationchunkholder.replaceProtoChunk(new ImposterProtoChunk(levelchunk, false));
            }

            levelchunk.setFullStatus(generationchunkholder::getFullStatus);
            levelchunk.runPostLoad();
            levelchunk.setLoaded(true);
            levelchunk.registerAllBlockEntitiesAfterLevelLoad();
            levelchunk.registerTickContainerInLevel(serverlevel);
            levelchunk.setUnsavedListener(p_342042_.unsavedListener());
            return levelchunk;
         },
         p_342042_.mainThreadExecutor()
      );
   }

   private static void postLoadProtoChunk(ServerLevel p_344060_, ValueInput.ValueInputList p_409754_) {
      if (!p_409754_.isEmpty()) {
         p_344060_.addWorldGenChunkEntities(EntityType.loadEntitiesRecursive(p_409754_, p_344060_, EntitySpawnReason.LOAD));
      }
   }
}
