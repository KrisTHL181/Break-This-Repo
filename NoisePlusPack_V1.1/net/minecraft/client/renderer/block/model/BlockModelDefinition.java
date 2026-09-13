package net.minecraft.client.renderer.block.model;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.UnmodifiableIterator;
import com.google.common.collect.ImmutableList.Builder;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import net.minecraft.client.renderer.block.model.multipart.MultiPartModel;
import net.minecraft.client.renderer.block.model.multipart.Selector;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.StateHolder;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public record BlockModelDefinition(
   Optional<BlockModelDefinition.SimpleModelSelectors> simpleModels, Optional<BlockModelDefinition.MultiPartDefinition> multiPart
) {
   static final Logger LOGGER = LogUtils.getLogger();
   public static final Codec<BlockModelDefinition> CODEC = RecordCodecBuilder.create(
         p_389470_ -> p_389470_.group(
               BlockModelDefinition.SimpleModelSelectors.CODEC.optionalFieldOf("variants").forGetter(BlockModelDefinition::simpleModels),
               BlockModelDefinition.MultiPartDefinition.CODEC.optionalFieldOf("multipart").forGetter(BlockModelDefinition::multiPart)
            )
            .apply(p_389470_, BlockModelDefinition::new)
      )
      .validate(
         p_389471_ -> p_389471_.simpleModels().isEmpty() && p_389471_.multiPart().isEmpty()
            ? DataResult.error(() -> "Neither 'variants' nor 'multipart' found")
            : DataResult.success(p_389471_)
      );

   public Map<BlockState, BlockStateModel.UnbakedRoot> instantiate(StateDefinition<Block, BlockState> p_361733_, Supplier<String> p_393858_) {
      Map<BlockState, BlockStateModel.UnbakedRoot> map = new IdentityHashMap<>();
      this.simpleModels.ifPresent(p_389469_ -> p_389469_.instantiate(p_361733_, p_393858_, (p_389473_, p_389474_) -> {
         BlockStateModel.UnbakedRoot blockstatemodel$unbakedroot = map.put(p_389473_, p_389474_);
         if (blockstatemodel$unbakedroot != null) {
            throw new IllegalArgumentException("Overlapping definition on state: " + p_389473_);
         }
      }));
      this.multiPart.ifPresent(p_389465_ -> {
         List<BlockState> list = p_361733_.getPossibleStates();
         BlockStateModel.UnbakedRoot blockstatemodel$unbakedroot = p_389465_.instantiate(p_361733_);

         for (BlockState blockstate : list) {
            map.putIfAbsent(blockstate, blockstatemodel$unbakedroot);
         }
      });
      return map;
   }

   @OnlyIn(Dist.CLIENT)
   public record MultiPartDefinition(List<Selector> selectors) {
      public static final Codec<BlockModelDefinition.MultiPartDefinition> CODEC = ExtraCodecs.nonEmptyList(Selector.CODEC.listOf())
         .xmap(BlockModelDefinition.MultiPartDefinition::new, BlockModelDefinition.MultiPartDefinition::selectors);

      public MultiPartModel.Unbaked instantiate(StateDefinition<Block, BlockState> p_392206_) {
         Builder<MultiPartModel.Selector<BlockStateModel.Unbaked>> builder = ImmutableList.builderWithExpectedSize(this.selectors.size());

         for (Selector selector : this.selectors) {
            builder.add(new MultiPartModel.Selector<>(selector.instantiate(p_392206_), selector.variant()));
         }

         return new MultiPartModel.Unbaked(builder.build());
      }
   }

   @OnlyIn(Dist.CLIENT)
   public record SimpleModelSelectors(Map<String, BlockStateModel.Unbaked> models) {
      public static final Codec<BlockModelDefinition.SimpleModelSelectors> CODEC = ExtraCodecs.nonEmptyMap(
            Codec.unboundedMap(Codec.STRING, BlockStateModel.Unbaked.CODEC)
         )
         .xmap(BlockModelDefinition.SimpleModelSelectors::new, BlockModelDefinition.SimpleModelSelectors::models);

      public void instantiate(
         StateDefinition<Block, BlockState> p_395902_, Supplier<String> p_394124_, BiConsumer<BlockState, BlockStateModel.UnbakedRoot> p_397088_
      ) {
         this.models
            .forEach(
               (p_398017_, p_395452_) -> {
                  try {
                     Predicate<StateHolder<Block, BlockState>> predicate = VariantSelector.predicate(p_395902_, p_398017_);
                     BlockStateModel.UnbakedRoot blockstatemodel$unbakedroot = p_395452_.asRoot();
                     UnmodifiableIterator var7 = p_395902_.getPossibleStates().iterator();

                     while (var7.hasNext()) {
                        BlockState blockstate = (BlockState)var7.next();
                        if (predicate.test(blockstate)) {
                           p_397088_.accept(blockstate, blockstatemodel$unbakedroot);
                        }
                     }
                  } catch (Exception exception) {
                     BlockModelDefinition.LOGGER
                        .warn(
                           "Exception loading blockstate definition: '{}' for variant: '{}': {}",
                           new Object[]{p_394124_.get(), p_398017_, exception.getMessage()}
                        );
                  }
               }
            );
      }
   }
}
