package net.minecraft.client.renderer.item;

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
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class RangeSelectItemModel implements ItemModel {
   private static final int LINEAR_SEARCH_THRESHOLD = 16;
   private final RangeSelectItemModelProperty property;
   private final float scale;
   private final float[] thresholds;
   private final ItemModel[] models;
   private final ItemModel fallback;

   RangeSelectItemModel(RangeSelectItemModelProperty p_378527_, float p_378148_, float[] p_377531_, ItemModel[] p_375411_, ItemModel p_378323_) {
      this.property = p_378527_;
      this.thresholds = p_377531_;
      this.models = p_375411_;
      this.fallback = p_378323_;
      this.scale = p_378148_;
   }

   private static int lastIndexLessOrEqual(float[] p_378298_, float p_378394_) {
      if (p_378298_.length < 16) {
         for (int k = 0; k < p_378298_.length; k++) {
            if (p_378298_[k] > p_378394_) {
               return k - 1;
            }
         }

         return p_378298_.length - 1;
      } else {
         int i = Arrays.binarySearch(p_378298_, p_378394_);
         if (i < 0) {
            int j = ~i;
            return j - 1;
         } else {
            return i;
         }
      }
   }

   @Override
   public void update(
      ItemStackRenderState p_376727_,
      ItemStack p_377507_,
      ItemModelResolver p_377370_,
      ItemDisplayContext p_377791_,
      @Nullable ClientLevel p_377343_,
      @Nullable ItemOwner p_427086_,
      int p_376528_
   ) {
      p_376727_.appendModelIdentityElement(this);
      float f = this.property.get(p_377507_, p_377343_, p_427086_, p_376528_) * this.scale;
      ItemModel itemmodel;
      if (Float.isNaN(f)) {
         itemmodel = this.fallback;
      } else {
         int i = lastIndexLessOrEqual(this.thresholds, f);
         itemmodel = i == -1 ? this.fallback : this.models[i];
      }

      itemmodel.update(p_376727_, p_377507_, p_377370_, p_377791_, p_377343_, p_427086_, p_376528_);
   }

   @OnlyIn(Dist.CLIENT)
   public record Entry(float threshold, ItemModel.Unbaked model) {
      public static final Codec<RangeSelectItemModel.Entry> CODEC = RecordCodecBuilder.create(
         p_375497_ -> p_375497_.group(
               Codec.FLOAT.fieldOf("threshold").forGetter(RangeSelectItemModel.Entry::threshold),
               ItemModels.CODEC.fieldOf("model").forGetter(RangeSelectItemModel.Entry::model)
            )
            .apply(p_375497_, RangeSelectItemModel.Entry::new)
      );
      public static final Comparator<RangeSelectItemModel.Entry> BY_THRESHOLD = Comparator.comparingDouble(RangeSelectItemModel.Entry::threshold);
   }

   @OnlyIn(Dist.CLIENT)
   public record Unbaked(RangeSelectItemModelProperty property, float scale, List<RangeSelectItemModel.Entry> entries, Optional<ItemModel.Unbaked> fallback)
      implements ItemModel.Unbaked {
      public static final MapCodec<RangeSelectItemModel.Unbaked> MAP_CODEC = RecordCodecBuilder.mapCodec(
         p_376755_ -> p_376755_.group(
               RangeSelectItemModelProperties.MAP_CODEC.forGetter(RangeSelectItemModel.Unbaked::property),
               Codec.FLOAT.optionalFieldOf("scale", 1.0F).forGetter(RangeSelectItemModel.Unbaked::scale),
               RangeSelectItemModel.Entry.CODEC.listOf().fieldOf("entries").forGetter(RangeSelectItemModel.Unbaked::entries),
               ItemModels.CODEC.optionalFieldOf("fallback").forGetter(RangeSelectItemModel.Unbaked::fallback)
            )
            .apply(p_376755_, RangeSelectItemModel.Unbaked::new)
      );

      @Override
      public MapCodec<RangeSelectItemModel.Unbaked> type() {
         return MAP_CODEC;
      }

      @Override
      public ItemModel bake(ItemModel.BakingContext p_378439_) {
         float[] afloat = new float[this.entries.size()];
         ItemModel[] aitemmodel = new ItemModel[this.entries.size()];
         List<RangeSelectItemModel.Entry> list = new ArrayList<>(this.entries);
         list.sort(RangeSelectItemModel.Entry.BY_THRESHOLD);

         for (int i = 0; i < list.size(); i++) {
            RangeSelectItemModel.Entry rangeselectitemmodel$entry = list.get(i);
            afloat[i] = rangeselectitemmodel$entry.threshold;
            aitemmodel[i] = rangeselectitemmodel$entry.model.bake(p_378439_);
         }

         ItemModel itemmodel = this.fallback.<ItemModel>map(p_375876_ -> p_375876_.bake(p_378439_)).orElse(p_378439_.missingItemModel());
         return new RangeSelectItemModel(this.property, this.scale, afloat, aitemmodel, itemmodel);
      }

      @Override
      public void resolveDependencies(ResolvableModel.Resolver p_378233_) {
         this.fallback.ifPresent(p_378531_ -> p_378531_.resolveDependencies(p_378233_));
         this.entries.forEach(p_377402_ -> p_377402_.model.resolveDependencies(p_378233_));
      }
   }
}
