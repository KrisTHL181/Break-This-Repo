package net.minecraft.client.renderer.block;

import com.mojang.math.Transformation;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.block.model.properties.select.SelectBlockModelProperty;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Matrix4fc;
import org.jspecify.annotations.Nullable;

public class SelectBlockModel<T> implements BlockModel {
   private final SelectBlockModelProperty<T> property;
   private final SelectBlockModel.ModelSelector<T> models;

   public SelectBlockModel(final SelectBlockModelProperty<T> property, final SelectBlockModel.ModelSelector<T> models) {
      this.property = property;
      this.models = models;
   }

   @Override
   public void update(final BlockModelRenderState output, final BlockState blockState, final BlockDisplayContext displayContext, final long seed) {
      T value = this.property.get(blockState, displayContext);
      BlockModel model = this.models.get(value);
      if (model != null) {
         model.update(output, blockState, displayContext, seed);
      }
   }

   @FunctionalInterface
   public interface ModelSelector<T> {
      @Nullable BlockModel get(@Nullable T value);
   }

   public record SwitchCase<T>(List<T> values, BlockModel.Unbaked model) {
   }

   public record Unbaked(Optional<Transformation> transformation, SelectBlockModel.UnbakedSwitch<?, ?> unbakedSwitch, Optional<BlockModel.Unbaked> fallback)
      implements BlockModel.Unbaked {
      @Override
      public BlockModel bake(final BlockModel.BakingContext context, final Matrix4fc transformation) {
         Matrix4fc childTransform = Transformation.compose(transformation, this.transformation);
         BlockModel bakedFallback = this.fallback.<BlockModel>map(m -> m.bake(context, childTransform)).orElse(context.missingBlockModel());
         return this.unbakedSwitch.bake(context, childTransform, bakedFallback);
      }
   }

   public record UnbakedSwitch<P extends SelectBlockModelProperty<T>, T>(P property, List<SelectBlockModel.SwitchCase<T>> cases) {
      public BlockModel bake(final BlockModel.BakingContext context, final Matrix4fc transformation, final BlockModel fallback) {
         Object2ObjectMap<T, BlockModel> bakedModels = new Object2ObjectOpenHashMap();

         for (SelectBlockModel.SwitchCase<T> c : this.cases) {
            BlockModel.Unbaked caseModel = c.model;
            BlockModel bakedCaseModel = caseModel.bake(context, transformation);

            for (T value : c.values) {
               bakedModels.put(value, bakedCaseModel);
            }
         }

         bakedModels.defaultReturnValue(fallback);
         return new SelectBlockModel<>(this.property, bakedModels::get);
      }
   }
}
