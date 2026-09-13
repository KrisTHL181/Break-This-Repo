package net.minecraft.world.level.levelgen.carver;

import java.util.Optional;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseChunk;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.SurfaceRules;
import net.minecraft.world.level.levelgen.WorldGenerationContext;

public class CarvingContext extends WorldGenerationContext {
   private final RegistryAccess registryAccess;
   private final NoiseChunk noiseChunk;
   private final RandomState randomState;
   private final SurfaceRules.RuleSource surfaceRule;

   public CarvingContext(
      NoiseBasedChunkGenerator p_224845_,
      RegistryAccess p_224846_,
      LevelHeightAccessor p_224847_,
      NoiseChunk p_224848_,
      RandomState p_224849_,
      SurfaceRules.RuleSource p_224850_
   ) {
      super(p_224845_, p_224847_);
      this.registryAccess = p_224846_;
      this.noiseChunk = p_224848_;
      this.randomState = p_224849_;
      this.surfaceRule = p_224850_;
   }

   @Deprecated
   public Optional<BlockState> topMaterial(Function<BlockPos, Holder<Biome>> p_190647_, ChunkAccess p_190648_, BlockPos p_190649_, boolean p_190650_) {
      return this.randomState.surfaceSystem().topMaterial(this.surfaceRule, this, p_190647_, p_190648_, this.noiseChunk, p_190649_, p_190650_);
   }

   @Deprecated
   public RegistryAccess registryAccess() {
      return this.registryAccess;
   }

   public RandomState randomState() {
      return this.randomState;
   }
}
