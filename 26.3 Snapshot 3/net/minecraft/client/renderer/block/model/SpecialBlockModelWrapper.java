package net.minecraft.client.renderer.block.model;

import com.mojang.math.Transformation;
import java.util.Optional;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Matrix4fc;

public class SpecialBlockModelWrapper<T> implements BlockModel {
   private final SpecialModelRenderer<T> specialRenderer;
   private final Matrix4fc transformation;

   public SpecialBlockModelWrapper(final SpecialModelRenderer<T> specialRenderer, final Matrix4fc transformation) {
      this.specialRenderer = specialRenderer;
      this.transformation = transformation;
   }

   @Override
   public void update(final BlockModelRenderState output, final BlockState blockState, final BlockDisplayContext displayContext, final long seed) {
      output.setupSpecialModel(this.specialRenderer, this.transformation);
   }

   public record Unbaked<T>(SpecialModelRenderer.Unbaked<T> model, Optional<Transformation> transformation) implements BlockModel.Unbaked {
      @Override
      public BlockModel bake(final BlockModel.BakingContext context, final Matrix4fc transformation) {
         SpecialModelRenderer<T> baked = this.model.bake(context);
         if (baked == null) {
            return EmptyBlockModel.INSTANCE;
         }

         Matrix4fc modelTransform = Transformation.compose(transformation, this.transformation);
         return new SpecialBlockModelWrapper<>(baked, modelTransform);
      }
   }
}
