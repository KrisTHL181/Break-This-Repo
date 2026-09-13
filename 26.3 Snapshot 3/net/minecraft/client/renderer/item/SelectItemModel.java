package net.minecraft.client.renderer.item;

import com.mojang.math.Transformation;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.multiplayer.CacheSlot;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.properties.select.SelectItemModelProperties;
import net.minecraft.client.renderer.item.properties.select.SelectItemModelProperty;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.RegistryContextSwapper;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4fc;
import org.jspecify.annotations.Nullable;

public class SelectItemModel<T> implements ItemModel {
   private final SelectItemModelProperty<T> property;
   private final SelectItemModel.ModelSelector<T> models;

   public SelectItemModel(final SelectItemModelProperty<T> property, final SelectItemModel.ModelSelector<T> models) {
      this.property = property;
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
      T value = this.property.get(item, level, owner == null ? null : owner.asLivingEntity(), seed, displayContext);
      this.models.get(value, level).update(output, item, resolver, displayContext, level, owner, seed);
   }

   @FunctionalInterface
   public interface ModelSelector<T> {
      ItemModel get(@Nullable T value, @Nullable ClientLevel context);
   }

   public record SwitchCase<T>(List<T> values, ItemModel.Unbaked model) {
      public static <T> Codec<SelectItemModel.SwitchCase<T>> codec(final Codec<T> valueCodec) {
         return RecordCodecBuilder.create(
            i -> i.group(
                  ExtraCodecs.nonEmptyList(ExtraCodecs.compactListCodec(valueCodec)).fieldOf("when").forGetter(SelectItemModel.SwitchCase::values),
                  ItemModels.CODEC.fieldOf("model").forGetter(SelectItemModel.SwitchCase::model)
               )
               .apply(i, SelectItemModel.SwitchCase::new)
         );
      }
   }

   public record Unbaked(Optional<Transformation> transformation, SelectItemModel.UnbakedSwitch<?, ?> unbakedSwitch, Optional<ItemModel.Unbaked> fallback)
      implements ItemModel.Unbaked {
      public static final MapCodec<SelectItemModel.Unbaked> MAP_CODEC = RecordCodecBuilder.mapCodec(
         i -> i.group(
               Transformation.EXTENDED_CODEC.optionalFieldOf("transformation").forGetter(SelectItemModel.Unbaked::transformation),
               SelectItemModel.UnbakedSwitch.MAP_CODEC.forGetter(SelectItemModel.Unbaked::unbakedSwitch),
               ItemModels.CODEC.optionalFieldOf("fallback").forGetter(SelectItemModel.Unbaked::fallback)
            )
            .apply(i, SelectItemModel.Unbaked::new)
      );

      @Override
      public MapCodec<SelectItemModel.Unbaked> type() {
         return MAP_CODEC;
      }

      @Override
      public ItemModel bake(final ItemModel.BakingContext context, final Matrix4fc transformation) {
         Matrix4fc childTransform = Transformation.compose(transformation, this.transformation);
         ItemModel bakedFallback = this.fallback.<ItemModel>map(m -> m.bake(context, childTransform)).orElseGet(() -> context.missingItemModel(childTransform));
         return this.unbakedSwitch.bake(context, childTransform, bakedFallback);
      }

      @Override
      public void resolveDependencies(final ResolvableModel.Resolver resolver) {
         this.unbakedSwitch.resolveDependencies(resolver);
         this.fallback.ifPresent(m -> m.resolveDependencies(resolver));
      }
   }

   public record UnbakedSwitch<P extends SelectItemModelProperty<T>, T>(P property, List<SelectItemModel.SwitchCase<T>> cases) {
      public static final MapCodec<SelectItemModel.UnbakedSwitch<?, ?>> MAP_CODEC = SelectItemModelProperties.CODEC
         .dispatchMap("property", unbaked -> unbaked.property().type(), SelectItemModelProperty.Type::switchCodec);

      public ItemModel bake(final ItemModel.BakingContext context, final Matrix4fc transformation, final ItemModel fallback) {
         Object2ObjectMap<T, ItemModel> bakedModels = new Object2ObjectOpenHashMap();

         for (SelectItemModel.SwitchCase<T> c : this.cases) {
            ItemModel.Unbaked caseModel = c.model;
            ItemModel bakedCaseModel = caseModel.bake(context, transformation);

            for (T value : c.values) {
               bakedModels.put(value, bakedCaseModel);
            }
         }

         bakedModels.defaultReturnValue(fallback);
         return new SelectItemModel<>(this.property, this.createModelGetter(bakedModels, context.contextSwapper()));
      }

      private SelectItemModel.ModelSelector<T> createModelGetter(
         final Object2ObjectMap<T, ItemModel> originalModels, final @Nullable RegistryContextSwapper registrySwapper
      ) {
         if (registrySwapper == null) {
            return (value, var2) -> (ItemModel)originalModels.get(value);
         }

         ItemModel defaultModel = (ItemModel)originalModels.defaultReturnValue();
         CacheSlot<ClientLevel, Object2ObjectMap<T, ItemModel>> remappedModelCache = new CacheSlot<>(
            clientLevel -> {
               Object2ObjectMap<T, ItemModel> remappedModels = new Object2ObjectOpenHashMap(originalModels.size());
               remappedModels.defaultReturnValue(defaultModel);
               originalModels.forEach(
                  (value, model) -> registrySwapper.swapTo(this.property.valueCodec(), value, clientLevel.registryAccess())
                     .ifSuccess(remappedValue -> remappedModels.put(remappedValue, model))
               );
               return remappedModels;
            }
         );
         return (value, context) -> {
            if (context == null) {
               return (ItemModel)originalModels.get(value);
            } else {
               return value == null ? defaultModel : (ItemModel)remappedModelCache.compute(context).get(value);
            }
         };
      }

      public void resolveDependencies(final ResolvableModel.Resolver resolver) {
         for (SelectItemModel.SwitchCase<?> c : this.cases) {
            c.model.resolveDependencies(resolver);
         }
      }
   }
}
