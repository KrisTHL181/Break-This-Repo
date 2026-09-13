package net.minecraft.client.renderer.block.model;

import com.mojang.math.Transformation;
import java.util.Optional;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.model.properties.conditional.ConditionalBlockModelProperty;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Matrix4fc;

public class ConditionalBlockModel implements BlockModel {
   private final ConditionalBlockModelProperty property;
   private final BlockModel onTrue;
   private final BlockModel onFalse;

   public ConditionalBlockModel(final ConditionalBlockModelProperty property, final BlockModel onTrue, final BlockModel onFalse) {
      this.property = property;
      this.onTrue = onTrue;
      this.onFalse = onFalse;
   }

   @Override
   public void update(final BlockModelRenderState output, final BlockState blockState, final BlockDisplayContext displayContext, final long seed) {
      (this.property.get(blockState) ? this.onTrue : this.onFalse).update(output, blockState, displayContext, seed);
   }

   public record Unbaked(Optional<Transformation> transformation, ConditionalBlockModelProperty property, BlockModel.Unbaked onTrue, BlockModel.Unbaked onFalse)
      implements BlockModel.Unbaked {
      @Override
      public BlockModel bake(final BlockModel.BakingContext context, final Matrix4fc transformation) {
         Matrix4fc childTransform = Transformation.compose(transformation, this.transformation);
         return new ConditionalBlockModel(this.property, this.onTrue.bake(context, childTransform), this.onFalse.bake(context, childTransform));
      }
   }
}
