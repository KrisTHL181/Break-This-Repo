package net.minecraft.world.level.block;

import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableList;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.apache.commons.lang3.function.TriFunction;

public record WeatheringCopperBlocks(
   Block unaffected, Block exposed, Block weathered, Block oxidized, Block waxed, Block waxedExposed, Block waxedWeathered, Block waxedOxidized
) {
   public static <WaxedBlock extends Block, WeatheringBlock extends Block & WeatheringCopper> WeatheringCopperBlocks create(
      String p_424012_,
      TriFunction<String, Function<BlockBehaviour.Properties, Block>, BlockBehaviour.Properties, Block> p_428693_,
      Function<BlockBehaviour.Properties, WaxedBlock> p_428959_,
      BiFunction<WeatheringCopper.WeatherState, BlockBehaviour.Properties, WeatheringBlock> p_427100_,
      Function<WeatheringCopper.WeatherState, BlockBehaviour.Properties> p_424277_
   ) {
      return new WeatheringCopperBlocks(
         (Block)p_428693_.apply(
            p_424012_,
            (Function<BlockBehaviour.Properties, Block>)p_425605_ -> p_427100_.apply(WeatheringCopper.WeatherState.UNAFFECTED, p_425605_),
            p_424277_.apply(WeatheringCopper.WeatherState.UNAFFECTED)
         ),
         (Block)p_428693_.apply(
            "exposed_" + p_424012_,
            (Function<BlockBehaviour.Properties, Block>)p_430944_ -> p_427100_.apply(WeatheringCopper.WeatherState.EXPOSED, p_430944_),
            p_424277_.apply(WeatheringCopper.WeatherState.EXPOSED)
         ),
         (Block)p_428693_.apply(
            "weathered_" + p_424012_,
            (Function<BlockBehaviour.Properties, Block>)p_431650_ -> p_427100_.apply(WeatheringCopper.WeatherState.WEATHERED, p_431650_),
            p_424277_.apply(WeatheringCopper.WeatherState.WEATHERED)
         ),
         (Block)p_428693_.apply(
            "oxidized_" + p_424012_,
            (Function<BlockBehaviour.Properties, Block>)p_430279_ -> p_427100_.apply(WeatheringCopper.WeatherState.OXIDIZED, p_430279_),
            p_424277_.apply(WeatheringCopper.WeatherState.OXIDIZED)
         ),
         (Block)p_428693_.apply("waxed_" + p_424012_, p_428959_::apply, p_424277_.apply(WeatheringCopper.WeatherState.UNAFFECTED)),
         (Block)p_428693_.apply("waxed_exposed_" + p_424012_, p_428959_::apply, p_424277_.apply(WeatheringCopper.WeatherState.EXPOSED)),
         (Block)p_428693_.apply("waxed_weathered_" + p_424012_, p_428959_::apply, p_424277_.apply(WeatheringCopper.WeatherState.WEATHERED)),
         (Block)p_428693_.apply("waxed_oxidized_" + p_424012_, p_428959_::apply, p_424277_.apply(WeatheringCopper.WeatherState.OXIDIZED))
      );
   }

   public ImmutableBiMap<Block, Block> weatheringMapping() {
      return ImmutableBiMap.of(this.unaffected, this.exposed, this.exposed, this.weathered, this.weathered, this.oxidized);
   }

   public ImmutableBiMap<Block, Block> waxedMapping() {
      return ImmutableBiMap.of(
         this.unaffected, this.waxed, this.exposed, this.waxedExposed, this.weathered, this.waxedWeathered, this.oxidized, this.waxedOxidized
      );
   }

   public ImmutableList<Block> asList() {
      return ImmutableList.of(
         this.unaffected, this.waxed, this.exposed, this.waxedExposed, this.weathered, this.waxedWeathered, this.oxidized, this.waxedOxidized
      );
   }

   public void forEach(Consumer<Block> p_423501_) {
      p_423501_.accept(this.unaffected);
      p_423501_.accept(this.exposed);
      p_423501_.accept(this.weathered);
      p_423501_.accept(this.oxidized);
      p_423501_.accept(this.waxed);
      p_423501_.accept(this.waxedExposed);
      p_423501_.accept(this.waxedWeathered);
      p_423501_.accept(this.waxedOxidized);
   }
}
