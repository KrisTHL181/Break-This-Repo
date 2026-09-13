package net.minecraft.world.level.block;

import com.google.common.base.Suppliers;
import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableBiMap.Builder;
import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import java.util.function.Supplier;
import java.util.stream.Stream;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.state.BlockState;

public interface WeatheringCopper extends ChangeOverTimeBlock<WeatheringCopper.WeatherState> {
   Supplier<BiMap<Block, Block>> NEXT_BY_BLOCK = Suppliers.memoize(
      () -> {
         Builder<Block, Block> builder = ImmutableBiMap.builder();
         Stream.of(
               Blocks.COPPER_BLOCK,
               Blocks.CUT_COPPER,
               Blocks.CHISELED_COPPER,
               Blocks.CUT_COPPER_SLAB,
               Blocks.CUT_COPPER_STAIRS,
               Blocks.COPPER_DOOR,
               Blocks.COPPER_TRAPDOOR,
               Blocks.COPPER_BARS,
               Blocks.COPPER_GRATE,
               Blocks.COPPER_BULB,
               Blocks.COPPER_LANTERN,
               Blocks.COPPER_CHEST,
               Blocks.COPPER_GOLEM_STATUE,
               Blocks.LIGHTNING_ROD,
               Blocks.COPPER_CHAIN
            )
            .forEach(collection -> collection.weathering().progressMapping(builder::put));
         return builder.build();
      }
   );
   Supplier<BiMap<Block, Block>> PREVIOUS_BY_BLOCK = Suppliers.memoize(() -> NEXT_BY_BLOCK.get().inverse());

   static Optional<Block> getPrevious(final Block block) {
      return Optional.ofNullable((Block)PREVIOUS_BY_BLOCK.get().get(block));
   }

   static Block getFirst(final Block block) {
      Block candiate = block;

      for (Block previous = (Block)PREVIOUS_BY_BLOCK.get().get(candiate); previous != null; previous = (Block)PREVIOUS_BY_BLOCK.get().get(candiate)) {
         candiate = previous;
      }

      return candiate;
   }

   static Optional<BlockState> getPrevious(final BlockState state) {
      return getPrevious(state.getBlock()).map(s -> s.withPropertiesOf(state));
   }

   static Optional<Block> getNext(final Block block) {
      return Optional.ofNullable((Block)NEXT_BY_BLOCK.get().get(block));
   }

   static BlockState getFirst(final BlockState state) {
      return getFirst(state.getBlock()).withPropertiesOf(state);
   }

   @Override
   default Optional<BlockState> getNext(final BlockState state) {
      return getNext(state.getBlock()).map(s -> s.withPropertiesOf(state));
   }

   @Override
   default float getChanceModifier() {
      return this.getAge() == WeatheringCopper.WeatherState.UNAFFECTED ? 0.75F : 1.0F;
   }

   enum WeatherState implements StringRepresentable {
      UNAFFECTED("unaffected"),
      EXPOSED("exposed"),
      WEATHERED("weathered"),
      OXIDIZED("oxidized");

      public static final IntFunction<WeatheringCopper.WeatherState> BY_ID = ByIdMap.continuous(Enum::ordinal, values(), ByIdMap.OutOfBoundsStrategy.CLAMP);
      public static final Codec<WeatheringCopper.WeatherState> CODEC = StringRepresentable.fromEnum(WeatheringCopper.WeatherState::values);
      public static final StreamCodec<ByteBuf, WeatheringCopper.WeatherState> STREAM_CODEC = ByteBufCodecs.idMapper(BY_ID, Enum::ordinal);
      private final String name;

      WeatherState(final String name) {
         this.name = name;
      }

      @Override
      public String getSerializedName() {
         return this.name;
      }

      public static void forEach(final Consumer<WeatheringCopper.WeatherState> consumer) {
         for (WeatheringCopper.WeatherState weatherState : values()) {
            consumer.accept(weatherState);
         }
      }

      public WeatheringCopper.WeatherState next() {
         return BY_ID.apply(this.ordinal() + 1);
      }

      public WeatheringCopper.WeatherState previous() {
         return BY_ID.apply(this.ordinal() - 1);
      }
   }
}
