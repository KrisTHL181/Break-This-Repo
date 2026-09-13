package net.minecraft.client.renderer.block.model;

import com.mojang.math.Transformation;
import it.unimi.dsi.fastutil.ints.IntList;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Matrix4fc;

public class BlockStateModelWrapper implements BlockModel {
   private final BlockStateModel model;
   private final List<BlockTintSource> tints;
   private final Matrix4fc transformation;

   public BlockStateModelWrapper(final BlockStateModel model, final List<BlockTintSource> tints, final Matrix4fc transformation) {
      this.model = model;
      this.tints = tints;
      this.transformation = transformation;
   }

   @Override
   public void update(final BlockModelRenderState output, final BlockState blockState, final BlockDisplayContext displayContext, final long seed) {
      List<BlockStateModelPart> partList = output.setupModel(this.transformation, this.model.hasMaterialFlag(1));
      this.model.collectParts(output.scratchRandomSource(seed), partList);
      this.updateTints(output, blockState);
   }

   private void updateTints(final BlockModelRenderState renderState, final BlockState blockState) {
      if (!this.tints.isEmpty()) {
         IntList tintLayers = renderState.tintLayers();

         for (BlockTintSource tint : this.tints) {
            tintLayers.add(tint.color(blockState));
         }
      }
   }

   public record Unbaked(BlockState model, List<BlockTintSource> tints, Optional<Transformation> transformation) implements BlockModel.Unbaked {
      @Override
      public BlockModel bake(final BlockModel.BakingContext context, final Matrix4fc transformation) {
         BlockStateModel baseModel = context.modelGetter().apply(this.model);
         Matrix4fc modelTransform = Transformation.compose(transformation, this.transformation);
         return new BlockStateModelWrapper(baseModel, this.tints, modelTransform);
      }
   }
}
