package net.minecraft.client.renderer.item;

import com.mojang.math.Transformation;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4fc;
import org.jspecify.annotations.Nullable;

public class CompositeModel implements ItemModel {
   private final List<ItemModel> models;

   public CompositeModel(final List<ItemModel> models) {
      this.models = models;
   }

   @Override
   public void update(
      final ItemStackRenderState output,
      final ItemStack item,
      final ItemModelResolver resolver,
      final ItemDisplayContext displayContext,
      final @Nullable ClientLevel level,
      final @Nullable ItemOwner owner,
      final int seed
   ) {
      output.appendModelIdentityElement(this);
      output.ensureCapacity(this.models.size());

      for (ItemModel model : this.models) {
         model.update(output, item, resolver, displayContext, level, owner, seed);
      }
   }

   public record Unbaked(List<ItemModel.Unbaked> models, Optional<Transformation> transformation) implements ItemModel.Unbaked {
      public static final MapCodec<CompositeModel.Unbaked> MAP_CODEC = RecordCodecBuilder.mapCodec(
         i -> i.group(
               ItemModels.CODEC.listOf().fieldOf("models").forGetter(CompositeModel.Unbaked::models),
               Transformation.EXTENDED_CODEC.optionalFieldOf("transformation").forGetter(CompositeModel.Unbaked::transformation)
            )
            .apply(i, CompositeModel.Unbaked::new)
      );

      @Override
      public MapCodec<CompositeModel.Unbaked> type() {
         return MAP_CODEC;
      }

      @Override
      public void resolveDependencies(final ResolvableModel.Resolver resolver) {
         for (ItemModel.Unbaked model : this.models) {
            model.resolveDependencies(resolver);
         }
      }

      @Override
      public ItemModel bake(final ItemModel.BakingContext context, final Matrix4fc transformation) {
         if (this.models.isEmpty()) {
            return EmptyModel.INSTANCE;
         }

         Matrix4fc childTransform = Transformation.compose(transformation, this.transformation);
         return this.models.size() == 1
            ? this.models.getFirst().bake(context, childTransform)
            : new CompositeModel(this.models.stream().map(m -> m.bake(context, childTransform)).toList());
      }
   }
}
