package net.minecraft.world.level.chunk.status;

import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;

public record ChunkPyramid(ImmutableList<ChunkStep> steps) {
   public static final ChunkPyramid GENERATION_PYRAMID = new ChunkPyramid.Builder()
      .step(ChunkStatus.EMPTY, s -> s)
      .step(ChunkStatus.STRUCTURE_STARTS, s -> s.setTask(ChunkStatusTasks::generateStructureStarts))
      .step(ChunkStatus.STRUCTURE_REFERENCES, s -> s.addRequirement(ChunkStatus.STRUCTURE_STARTS, 8).setTask(ChunkStatusTasks::generateStructureReferences))
      .step(ChunkStatus.BIOMES, s -> s.addRequirement(ChunkStatus.STRUCTURE_STARTS, 8).setTask(ChunkStatusTasks::generateBiomes))
      .step(
         ChunkStatus.NOISE,
         s -> s.addRequirement(ChunkStatus.STRUCTURE_STARTS, 8)
            .addRequirement(ChunkStatus.BIOMES, 1)
            .blockStateWriteRadius(0)
            .setTask(ChunkStatusTasks::generateNoise)
      )
      .step(
         ChunkStatus.SURFACE,
         s -> s.addRequirement(ChunkStatus.STRUCTURE_STARTS, 8)
            .addRequirement(ChunkStatus.BIOMES, 1)
            .blockStateWriteRadius(0)
            .setTask(ChunkStatusTasks::generateSurface)
      )
      .step(ChunkStatus.CARVERS, s -> s.addRequirement(ChunkStatus.STRUCTURE_STARTS, 8).blockStateWriteRadius(0).setTask(ChunkStatusTasks::generateCarvers))
      .step(
         ChunkStatus.FEATURES,
         s -> s.addRequirement(ChunkStatus.STRUCTURE_STARTS, 8)
            .addRequirement(ChunkStatus.CARVERS, 1)
            .blockStateWriteRadius(1)
            .setTask(ChunkStatusTasks::generateFeatures)
      )
      .step(ChunkStatus.INITIALIZE_LIGHT, s -> s.setTask(ChunkStatusTasks::initializeLight))
      .step(ChunkStatus.LIGHT, s -> s.addRequirement(ChunkStatus.INITIALIZE_LIGHT, 1).setTask(ChunkStatusTasks::light))
      .step(ChunkStatus.SPAWN, s -> s.addRequirement(ChunkStatus.BIOMES, 1).setTask(ChunkStatusTasks::generateSpawn))
      .step(ChunkStatus.FULL, s -> s.setTask(ChunkStatusTasks::full))
      .build();
   public static final ChunkPyramid LOADING_PYRAMID = new ChunkPyramid.Builder()
      .step(ChunkStatus.EMPTY, s -> s)
      .step(ChunkStatus.STRUCTURE_STARTS, s -> s.setTask(ChunkStatusTasks::loadStructureStarts))
      .step(ChunkStatus.STRUCTURE_REFERENCES, s -> s)
      .step(ChunkStatus.BIOMES, s -> s)
      .step(ChunkStatus.NOISE, s -> s)
      .step(ChunkStatus.SURFACE, s -> s)
      .step(ChunkStatus.CARVERS, s -> s)
      .step(ChunkStatus.FEATURES, s -> s)
      .step(ChunkStatus.INITIALIZE_LIGHT, s -> s.setTask(ChunkStatusTasks::initializeLight))
      .step(ChunkStatus.LIGHT, s -> s.addRequirement(ChunkStatus.INITIALIZE_LIGHT, 1).setTask(ChunkStatusTasks::light))
      .step(ChunkStatus.SPAWN, s -> s)
      .step(ChunkStatus.FULL, s -> s.setTask(ChunkStatusTasks::full))
      .build();
   private static final int SAFETY_MARGIN_CHUNKS = (32 + GENERATION_PYRAMID.getStepTo(ChunkStatus.FULL).accumulatedDependencies().size() + 1) * 2;
   public static final int MAX_CHUNK_COORDINATE_VALUE = SectionPos.blockToSectionCoord(BlockPos.MAX_HORIZONTAL_COORDINATE) - SAFETY_MARGIN_CHUNKS;

   public ChunkStep getStepTo(final ChunkStatus status) {
      return (ChunkStep)this.steps.get(status.getIndex());
   }

   public static class Builder {
      private final List<ChunkStep> steps = new ArrayList<>();

      public ChunkPyramid build() {
         return new ChunkPyramid(ImmutableList.copyOf(this.steps));
      }

      public ChunkPyramid.Builder step(final ChunkStatus status, final UnaryOperator<ChunkStep.Builder> operator) {
         ChunkStep.Builder stepBuilder;
         if (this.steps.isEmpty()) {
            stepBuilder = new ChunkStep.Builder(status);
         } else {
            stepBuilder = new ChunkStep.Builder(status, this.steps.getLast());
         }

         this.steps.add(operator.apply(stepBuilder).build());
         return this;
      }
   }
}
