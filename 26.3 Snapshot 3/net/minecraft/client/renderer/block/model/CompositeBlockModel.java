package net.minecraft.client.renderer.block.model;

import com.mojang.math.Transformation;
import java.util.Optional;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Matrix4fc;

public class CompositeBlockModel implements BlockModel {
   private final BlockModel normal;
   private final BlockModel custom;

   public CompositeBlockModel(final BlockModel normal, final BlockModel custom) {
      this.normal = normal;
      this.custom = custom;
   }

   @Override
   public void update(final BlockModelRenderState output, final BlockState blockState, final BlockDisplayContext displayContext, final long seed) {
      this.normal.update(output, blockState, displayContext, seed);
      this.custom.update(output, blockState, displayContext, seed);
   }

   public record Unbaked(BlockModel.Unbaked normal, BlockModel.Unbaked custom, Optional<Transformation> transformation) implements BlockModel.Unbaked {
      @Override
      public BlockModel bake(final BlockModel.BakingContext context, final Matrix4fc transformation) {
         Matrix4fc childTransform = Transformation.compose(transformation, this.transformation);
         return new CompositeBlockModel(this.normal.bake(context, childTransform), this.custom.bake(context, childTransform));
      }
   }
}
