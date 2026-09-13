package net.minecraft.client.renderer.item;

import com.mojang.math.Transformation;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.properties.numeric.RangeSelectItemModelProperties;
import net.minecraft.client.renderer.item.properties.numeric.RangeSelectItemModelProperty;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4fc;
import org.jspecify.annotations.Nullable;

public class RangeSelectItemModel implements ItemModel {
   private static final int LINEAR_SEARCH_THRESHOLD = 16;
   private final RangeSelectItemModelProperty property;
   private final float scale;
   private final float[] thresholds;
   private final ItemModel[] models;
   private final ItemModel fallback;

   private RangeSelectItemModel(
      final RangeSelectItemModelProperty property, final float scale, final float[] thresholds, final ItemModel[] models, final ItemModel fallback
   ) {
      this.property = property;
      this.thresholds = thresholds;
      this.models = models;
      this.fallback = fallback;
      this.scale = scale;
   }

   private static int lastIndexLessOrEqual(final float[] haystack, final float needle) {
      if (haystack.length < 16) {
         for (int i = 0; i < haystack.length; i++) {
            if (haystack[i] > needle) {
               return i - 1;
            }
         }

         return haystack.length - 1;
      } else {
         int index = Arrays.binarySearch(haystack, needle);
         if (index < 0) {
            int insertionPoint = ~index;
            return insertionPoint - 1;
         } else {
            return index;
         }
      }
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
      float value = this.property.get(item, level, owner, seed) * this.scale;
      ItemModel selectedModel;
      if (Float.isNaN(value)) {
         selectedModel = this.fallback;
      } else {
         int index = lastIndexLessOrEqual(this.thresholds, value);
         selectedModel = index == -1 ? this.fallback : this.models[index];
      }

      selectedModel.update(output, item, resolver, displayContext, level, owner, seed);
   }

   public record Entry(float threshold, ItemModel.Unbaked model) {
      public static final Codec<RangeSelectItemModel.Entry> CODEC = RecordCodecBuilder.create(
         i -> i.group(
               Codec.FLOAT.fieldOf("threshold").forGetter(RangeSelectItemModel.Entry::threshold),
               ItemModels.CODEC.fieldOf("model").forGetter(RangeSelectItemModel.Entry::model)
            )
            .apply(i, RangeSelectItemModel.Entry::new)
      );
      public static final Comparator<RangeSelectItemModel.Entry> BY_THRESHOLD = Comparator.comparingDouble(RangeSelectItemModel.Entry::threshold);
   }

   public record Unbaked(
      Optional<Transformation> transformation,
      RangeSelectItemModelProperty property,
      float scale,
      List<RangeSelectItemModel.Entry> entries,
      Optional<ItemModel.Unbaked> fallback
   ) implements ItemModel.Unbaked {
      public static final MapCodec<RangeSelectItemModel.Unbaked> MAP_CODEC = RecordCodecBuilder.mapCodec(
         i -> i.group(
               Transformation.EXTENDED_CODEC.optionalFieldOf("transformation").forGetter(RangeSelectItemModel.Unbaked::transformation),
               RangeSelectItemModelProperties.MAP_CODEC.forGetter(RangeSelectItemModel.Unbaked::property),
               Codec.FLOAT.optionalFieldOf("scale", 1.0F).forGetter(RangeSelectItemModel.Unbaked::scale),
               RangeSelectItemModel.Entry.CODEC.listOf().fieldOf("entries").forGetter(RangeSelectItemModel.Unbaked::entries),
               ItemModels.CODEC.optionalFieldOf("fallback").forGetter(RangeSelectItemModel.Unbaked::fallback)
            )
            .apply(i, RangeSelectItemModel.Unbaked::new)
      );

      @Override
      public MapCodec<RangeSelectItemModel.Unbaked> type() {
         return MAP_CODEC;
      }

      @Override
      public ItemModel bake(final ItemModel.BakingContext context, final Matrix4fc transformation) {
         Matrix4fc childTransform = Transformation.compose(transformation, this.transformation);
         float[] thresholds = new float[this.entries.size()];
         ItemModel[] models = new ItemModel[this.entries.size()];
         List<RangeSelectItemModel.Entry> mutableEntries = new ArrayList<>(this.entries);
         mutableEntries.sort(RangeSelectItemModel.Entry.BY_THRESHOLD);

         for (int i = 0; i < mutableEntries.size(); i++) {
            RangeSelectItemModel.Entry entry = mutableEntries.get(i);
            thresholds[i] = entry.threshold;
            models[i] = entry.model.bake(context, childTransform);
         }

         ItemModel bakedFallback = this.fallback.<ItemModel>map(m -> m.bake(context, childTransform)).orElseGet(() -> context.missingItemModel(childTransform));
         return new RangeSelectItemModel(this.property, this.scale, thresholds, models, bakedFallback);
      }

      @Override
      public void resolveDependencies(final ResolvableModel.Resolver resolver) {
         this.fallback.ifPresent(m -> m.resolveDependencies(resolver));
         this.entries.forEach(entry -> entry.model.resolveDependencies(resolver));
      }
   }
}
