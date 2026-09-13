package net.minecraft.client.renderer.item;

import com.mojang.math.Transformation;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.client.multiplayer.CacheSlot;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.properties.conditional.ConditionalItemModelProperties;
import net.minecraft.client.renderer.item.properties.conditional.ConditionalItemModelProperty;
import net.minecraft.client.renderer.item.properties.conditional.ItemModelPropertyTest;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.util.RegistryContextSwapper;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4fc;
import org.jspecify.annotations.Nullable;

public class ConditionalItemModel implements ItemModel {
   private final ItemModelPropertyTest property;
   private final ItemModel onTrue;
   private final ItemModel onFalse;

   public ConditionalItemModel(final ItemModelPropertyTest property, final ItemModel onTrue, final ItemModel onFalse) {
      this.property = property;
      this.onTrue = onTrue;
      this.onFalse = onFalse;
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
      (this.property.get(item, level, owner == null ? null : owner.asLivingEntity(), seed, displayContext) ? this.onTrue : this.onFalse)
         .update(output, item, resolver, displayContext, level, owner, seed);
   }

   public record Unbaked(Optional<Transformation> transformation, ConditionalItemModelProperty property, ItemModel.Unbaked onTrue, ItemModel.Unbaked onFalse)
      implements ItemModel.Unbaked {
      public static final MapCodec<ConditionalItemModel.Unbaked> MAP_CODEC = RecordCodecBuilder.mapCodec(
         i -> i.group(
               Transformation.EXTENDED_CODEC.optionalFieldOf("transformation").forGetter(ConditionalItemModel.Unbaked::transformation),
               ConditionalItemModelProperties.MAP_CODEC.forGetter(ConditionalItemModel.Unbaked::property),
               ItemModels.CODEC.fieldOf("on_true").forGetter(ConditionalItemModel.Unbaked::onTrue),
               ItemModels.CODEC.fieldOf("on_false").forGetter(ConditionalItemModel.Unbaked::onFalse)
            )
            .apply(i, ConditionalItemModel.Unbaked::new)
      );

      @Override
      public MapCodec<ConditionalItemModel.Unbaked> type() {
         return MAP_CODEC;
      }

      @Override
      public ItemModel bake(final ItemModel.BakingContext context, final Matrix4fc transformation) {
         Matrix4fc childTransform = Transformation.compose(transformation, this.transformation);
         return new ConditionalItemModel(
            this.adaptProperty(this.property, context.contextSwapper()), this.onTrue.bake(context, childTransform), this.onFalse.bake(context, childTransform)
         );
      }

      private ItemModelPropertyTest adaptProperty(final ConditionalItemModelProperty originalProperty, final @Nullable RegistryContextSwapper contextSwapper) {
         if (contextSwapper == null) {
            return originalProperty;
         }

         CacheSlot<ClientLevel, ItemModelPropertyTest> remappedModelCache = new CacheSlot<>(context -> swapContext(originalProperty, contextSwapper, context));
         return (itemStack, level, owner, seed, displayContext) -> {
            ItemModelPropertyTest property = level == null ? originalProperty : remappedModelCache.compute(level);
            return property.get(itemStack, level, owner, seed, displayContext);
         };
      }

      private static <T extends ConditionalItemModelProperty> T swapContext(
         final T originalProperty, final RegistryContextSwapper contextSwapper, final ClientLevel context
      ) {
         return (T)contextSwapper.swapTo(originalProperty.type().codec(), originalProperty, context.registryAccess()).result().orElse(originalProperty);
      }

      @Override
      public void resolveDependencies(final ResolvableModel.Resolver resolver) {
         this.onTrue.resolveDependencies(resolver);
         this.onFalse.resolveDependencies(resolver);
      }
   }
}
