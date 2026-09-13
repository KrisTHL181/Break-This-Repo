package net.minecraft.client.renderer.block.model;

import com.google.common.collect.Lists;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.client.resources.model.WeightedVariants;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.RandomSource;
import net.minecraft.util.random.Weighted;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public interface BlockStateModel {
   void collectParts(RandomSource var1, List<BlockModelPart> var2);

   default List<BlockModelPart> collectParts(RandomSource p_392713_) {
      List<BlockModelPart> list = new ObjectArrayList();
      this.collectParts(p_392713_, list);
      return list;
   }

   TextureAtlasSprite particleIcon();

   @OnlyIn(Dist.CLIENT)
   class SimpleCachedUnbakedRoot implements BlockStateModel.UnbakedRoot {
      final BlockStateModel.Unbaked contents;
      private final ModelBaker.SharedOperationKey<BlockStateModel> bakingKey = new ModelBaker.SharedOperationKey<BlockStateModel>() {
         public BlockStateModel compute(ModelBaker p_396245_) {
            return SimpleCachedUnbakedRoot.this.contents.bake(p_396245_);
         }
      };

      public SimpleCachedUnbakedRoot(BlockStateModel.Unbaked p_394126_) {
         this.contents = p_394126_;
      }

      @Override
      public void resolveDependencies(ResolvableModel.Resolver p_396058_) {
         this.contents.resolveDependencies(p_396058_);
      }

      @Override
      public BlockStateModel bake(BlockState p_394850_, ModelBaker p_396441_) {
         return p_396441_.compute(this.bakingKey);
      }

      @Override
      public Object visualEqualityGroup(BlockState p_395333_) {
         return this;
      }
   }

   @OnlyIn(Dist.CLIENT)
   interface Unbaked extends ResolvableModel {
      Codec<Weighted<Variant>> ELEMENT_CODEC = RecordCodecBuilder.create(
         p_396421_ -> p_396421_.group(
               Variant.MAP_CODEC.forGetter(Weighted::value), ExtraCodecs.POSITIVE_INT.optionalFieldOf("weight", 1).forGetter(Weighted::weight)
            )
            .apply(p_396421_, Weighted::new)
      );
      Codec<WeightedVariants.Unbaked> HARDCODED_WEIGHTED_CODEC = ExtraCodecs.nonEmptyList(ELEMENT_CODEC.listOf())
         .flatComapMap(
            p_393029_ -> new WeightedVariants.Unbaked(WeightedList.of(Lists.transform(p_393029_, p_392845_ -> p_392845_.map(SingleVariant.Unbaked::new)))),
            p_391675_ -> {
               List<Weighted<BlockStateModel.Unbaked>> list = p_391675_.entries().unwrap();
               List<Weighted<Variant>> list1 = new ArrayList<>(list.size());

               for (Weighted<BlockStateModel.Unbaked> weighted : list) {
                  if (!(weighted.value() instanceof SingleVariant.Unbaked singlevariant$unbaked)) {
                     return DataResult.error(() -> "Only single variants are supported");
                  }

                  list1.add(new Weighted<>(singlevariant$unbaked.variant(), weighted.weight()));
               }

               return DataResult.success(list1);
            }
         );
      Codec<BlockStateModel.Unbaked> CODEC = Codec.either(HARDCODED_WEIGHTED_CODEC, SingleVariant.Unbaked.CODEC)
         .flatComapMap(p_394601_ -> (BlockStateModel.Unbaked)p_394601_.map(p_396155_ -> p_396155_, p_397083_ -> p_397083_), p_392163_ -> {
            return switch (p_392163_) {
               case SingleVariant.Unbaked singlevariant$unbaked -> DataResult.success(Either.right(singlevariant$unbaked));
               case WeightedVariants.Unbaked weightedvariants$unbaked -> DataResult.success(Either.left(weightedvariants$unbaked));
               default -> DataResult.error(() -> "Only a single variant or a list of variants are supported");
            };
         });

      BlockStateModel bake(ModelBaker var1);

      default BlockStateModel.UnbakedRoot asRoot() {
         return new BlockStateModel.SimpleCachedUnbakedRoot(this);
      }
   }

   @OnlyIn(Dist.CLIENT)
   interface UnbakedRoot extends ResolvableModel {
      BlockStateModel bake(BlockState var1, ModelBaker var2);

      Object visualEqualityGroup(BlockState var1);
   }
}
