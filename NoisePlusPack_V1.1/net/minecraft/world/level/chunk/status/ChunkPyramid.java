package net.minecraft.world.level.chunk.status;

import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;

public record ChunkPyramid(ImmutableList<ChunkStep> steps) {
   public static final ChunkPyramid GENERATION_PYRAMID = new ChunkPyramid.Builder()
      .step(ChunkStatus.EMPTY, p_342975_ -> p_342975_)
      .step(ChunkStatus.STRUCTURE_STARTS, p_342544_ -> p_342544_.setTask(ChunkStatusTasks::generateStructureStarts))
      .step(
         ChunkStatus.STRUCTURE_REFERENCES,
         p_345155_ -> p_345155_.addRequirement(ChunkStatus.STRUCTURE_STARTS, 8).setTask(ChunkStatusTasks::generateStructureReferences)
      )
      .step(ChunkStatus.BIOMES, p_342684_ -> p_342684_.addRequirement(ChunkStatus.STRUCTURE_STARTS, 8).setTask(ChunkStatusTasks::generateBiomes))
      .step(
         ChunkStatus.NOISE,
         p_344472_ -> p_344472_.addRequirement(ChunkStatus.STRUCTURE_STARTS, 8)
            .addRequirement(ChunkStatus.BIOMES, 1)
            .blockStateWriteRadius(0)
            .setTask(ChunkStatusTasks::generateNoise)
      )
      .step(
         ChunkStatus.SURFACE,
         p_345476_ -> p_345476_.addRequirement(ChunkStatus.STRUCTURE_STARTS, 8)
            .addRequirement(ChunkStatus.BIOMES, 1)
            .blockStateWriteRadius(0)
            .setTask(ChunkStatusTasks::generateSurface)
      )
      .step(
         ChunkStatus.CARVERS,
         p_343920_ -> p_343920_.addRequirement(ChunkStatus.STRUCTURE_STARTS, 8).blockStateWriteRadius(0).setTask(ChunkStatusTasks::generateCarvers)
      )
      .step(
         ChunkStatus.FEATURES,
         p_345027_ -> p_345027_.addRequirement(ChunkStatus.STRUCTURE_STARTS, 8)
            .addRequirement(ChunkStatus.CARVERS, 1)
            .blockStateWriteRadius(1)
            .setTask(ChunkStatusTasks::generateFeatures)
      )
      .step(ChunkStatus.INITIALIZE_LIGHT, p_342175_ -> p_342175_.setTask(ChunkStatusTasks::initializeLight))
      .step(ChunkStatus.LIGHT, p_342930_ -> p_342930_.addRequirement(ChunkStatus.INITIALIZE_LIGHT, 1).setTask(ChunkStatusTasks::light))
      .step(ChunkStatus.SPAWN, p_345462_ -> p_345462_.addRequirement(ChunkStatus.BIOMES, 1).setTask(ChunkStatusTasks::generateSpawn))
      .step(ChunkStatus.FULL, p_343894_ -> p_343894_.setTask(ChunkStatusTasks::full))
      .build();
   public static final ChunkPyramid LOADING_PYRAMID = new ChunkPyramid.Builder()
      .step(ChunkStatus.EMPTY, p_342764_ -> p_342764_)
      .step(ChunkStatus.STRUCTURE_STARTS, p_345203_ -> p_345203_.setTask(ChunkStatusTasks::loadStructureStarts))
      .step(ChunkStatus.STRUCTURE_REFERENCES, p_344362_ -> p_344362_)
      .step(ChunkStatus.BIOMES, p_344572_ -> p_344572_)
      .step(ChunkStatus.NOISE, p_343829_ -> p_343829_)
      .step(ChunkStatus.SURFACE, p_345115_ -> p_345115_)
      .step(ChunkStatus.CARVERS, p_342473_ -> p_342473_)
      .step(ChunkStatus.FEATURES, p_343425_ -> p_343425_)
      .step(ChunkStatus.INITIALIZE_LIGHT, p_343066_ -> p_343066_.setTask(ChunkStatusTasks::initializeLight))
      .step(ChunkStatus.LIGHT, p_342741_ -> p_342741_.addRequirement(ChunkStatus.INITIALIZE_LIGHT, 1).setTask(ChunkStatusTasks::light))
      .step(ChunkStatus.SPAWN, p_342632_ -> p_342632_)
      .step(ChunkStatus.FULL, p_343704_ -> p_343704_.setTask(ChunkStatusTasks::full))
      .build();

   public ChunkStep getStepTo(ChunkStatus p_343202_) {
      return (ChunkStep)this.steps.get(p_343202_.getIndex());
   }

   public static class Builder {
      private final List<ChunkStep> steps = new ArrayList<>();

      public ChunkPyramid build() {
         return new ChunkPyramid(ImmutableList.copyOf(this.steps));
      }

      public ChunkPyramid.Builder step(ChunkStatus p_342085_, UnaryOperator<ChunkStep.Builder> p_342673_) {
         ChunkStep.Builder chunkstep$builder;
         if (this.steps.isEmpty()) {
            chunkstep$builder = new ChunkStep.Builder(p_342085_);
         } else {
            chunkstep$builder = new ChunkStep.Builder(p_342085_, this.steps.getLast());
         }

         this.steps.add(p_342673_.apply(chunkstep$builder).build());
         return this;
      }
   }
}
