package net.minecraft.world.level.levelgen.material;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.NoiseChunk;
import org.jspecify.annotations.Nullable;

public record MaterialRuleList(NoiseChunk.BlockStateFiller[] materialRuleList) implements NoiseChunk.BlockStateFiller {
   @Override
   public @Nullable BlockState calculate(DensityFunction.FunctionContext p_209815_) {
      for (NoiseChunk.BlockStateFiller noisechunk$blockstatefiller : this.materialRuleList) {
         BlockState blockstate = noisechunk$blockstatefiller.calculate(p_209815_);
         if (blockstate != null) {
            return blockstate;
         }
      }

      return null;
   }
}
