package net.minecraft.world.level.levelgen.material;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.NoiseChunk;
import org.jspecify.annotations.Nullable;

public record MaterialRuleList(NoiseChunk.BlockStateFiller[] materialRuleList) implements NoiseChunk.BlockStateFiller {
   @Override
   public @Nullable BlockState calculate(final DensityFunction.FunctionContext context) {
      for (NoiseChunk.BlockStateFiller rule : this.materialRuleList) {
         BlockState state = rule.calculate(context);
         if (state != null) {
            return state;
         }
      }

      return null;
   }
}
