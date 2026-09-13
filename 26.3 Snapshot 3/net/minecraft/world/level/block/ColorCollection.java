package net.minecraft.world.level.block;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.apache.commons.lang3.function.TriFunction;

public record ColorCollection<T>(
   T white, T orange, T magenta, T lightBlue, T yellow, T lime, T pink, T gray, T lightGray, T cyan, T purple, T blue, T brown, T green, T red, T black
) {
   public static final ColorCollection<DyeColor> VALUES = new ColorCollection<>(
      DyeColor.WHITE,
      DyeColor.ORANGE,
      DyeColor.MAGENTA,
      DyeColor.LIGHT_BLUE,
      DyeColor.YELLOW,
      DyeColor.LIME,
      DyeColor.PINK,
      DyeColor.GRAY,
      DyeColor.LIGHT_GRAY,
      DyeColor.CYAN,
      DyeColor.PURPLE,
      DyeColor.BLUE,
      DyeColor.BROWN,
      DyeColor.GREEN,
      DyeColor.RED,
      DyeColor.BLACK
   );
   public static final ColorCollection<String> NAMES = VALUES.map(DyeColor::getName);

   public static <T> ColorCollection<T> create(final T value) {
      return new ColorCollection<>(value, value, value, value, value, value, value, value, value, value, value, value, value, value, value, value);
   }

   public static <B extends Block, Id> ColorCollection<Block> registerBlocks(
      final ColorCollection<Id> ids,
      final TriFunction<Id, Function<BlockBehaviour.Properties, Block>, BlockBehaviour.Properties, Block> register,
      final BiFunction<DyeColor, BlockBehaviour.Properties, B> colorBlockFactory,
      final Function<DyeColor, BlockBehaviour.Properties> propertiesSupplier
   ) {
      return zipMap(
         VALUES,
         ids,
         (color, id) -> (Block)register.apply(
            id, (Function<BlockBehaviour.Properties, Block>)p -> colorBlockFactory.apply(color, p), propertiesSupplier.apply(color)
         )
      );
   }

   public static <Id> ColorCollection<Item> registerBlockItems(
      final ColorCollection<Id> ids, final ColorCollection<Block> blocks, final TriFunction<Id, Block, DyeColor, Item> itemFactory
   ) {
      return zipMap(VALUES, ids, (color, id) -> (Item)itemFactory.apply(id, blocks.pick(color), color));
   }

   public static <Id> ColorCollection<Item> registerItems(final ColorCollection<Id> ids, final BiFunction<Id, DyeColor, Item> itemFactory) {
      return zipMap(VALUES, ids, (color, id) -> itemFactory.apply(id, color));
   }

   public static ColorCollection<String> prefixWithColor(final ColorCollection<String> ids) {
      return zipMap(NAMES, ids, (color, id) -> color + "_" + id);
   }

   public List<T> asList() {
      Builder<T> builder = ImmutableList.builderWithExpectedSize(16);
      this.forEach(builder::add);
      return builder.build();
   }

   public void forEach(final Consumer<T> consumer) {
      consumer.accept(this.white);
      consumer.accept(this.orange);
      consumer.accept(this.magenta);
      consumer.accept(this.lightBlue);
      consumer.accept(this.yellow);
      consumer.accept(this.lime);
      consumer.accept(this.pink);
      consumer.accept(this.gray);
      consumer.accept(this.lightGray);
      consumer.accept(this.cyan);
      consumer.accept(this.purple);
      consumer.accept(this.blue);
      consumer.accept(this.brown);
      consumer.accept(this.green);
      consumer.accept(this.red);
      consumer.accept(this.black);
   }

   public T pick(final DyeColor dyeColor) {
      return (T)(switch (dyeColor) {
         case WHITE -> this.white;
         case ORANGE -> this.orange;
         case MAGENTA -> this.magenta;
         case LIGHT_BLUE -> this.lightBlue;
         case YELLOW -> this.yellow;
         case LIME -> this.lime;
         case PINK -> this.pink;
         case GRAY -> this.gray;
         case LIGHT_GRAY -> this.lightGray;
         case CYAN -> this.cyan;
         case PURPLE -> this.purple;
         case BLUE -> this.blue;
         case BROWN -> this.brown;
         case GREEN -> this.green;
         case RED -> this.red;
         case BLACK -> this.black;
      });
   }

   public <U> ColorCollection<U> map(final Function<T, U> mapper) {
      return new ColorCollection<>(
         mapper.apply(this.white),
         mapper.apply(this.orange),
         mapper.apply(this.magenta),
         mapper.apply(this.lightBlue),
         mapper.apply(this.yellow),
         mapper.apply(this.lime),
         mapper.apply(this.pink),
         mapper.apply(this.gray),
         mapper.apply(this.lightGray),
         mapper.apply(this.cyan),
         mapper.apply(this.purple),
         mapper.apply(this.blue),
         mapper.apply(this.brown),
         mapper.apply(this.green),
         mapper.apply(this.red),
         mapper.apply(this.black)
      );
   }

   public static <T, U> void zipApply(final ColorCollection<T> first, final ColorCollection<U> second, final BiConsumer<T, U> consumer) {
      consumer.accept(first.white(), second.white());
      consumer.accept(first.orange(), second.orange());
      consumer.accept(first.magenta(), second.magenta());
      consumer.accept(first.lightBlue(), second.lightBlue());
      consumer.accept(first.yellow(), second.yellow());
      consumer.accept(first.lime(), second.lime());
      consumer.accept(first.pink(), second.pink());
      consumer.accept(first.gray(), second.gray());
      consumer.accept(first.lightGray(), second.lightGray());
      consumer.accept(first.cyan(), second.cyan());
      consumer.accept(first.purple(), second.purple());
      consumer.accept(first.blue(), second.blue());
      consumer.accept(first.brown(), second.brown());
      consumer.accept(first.green(), second.green());
      consumer.accept(first.red(), second.red());
      consumer.accept(first.black(), second.black());
   }

   public static <T, U, R> ColorCollection<R> zipMap(final ColorCollection<T> first, final ColorCollection<U> second, final BiFunction<T, U, R> operation) {
      return new ColorCollection<>(
         operation.apply(first.white(), second.white()),
         operation.apply(first.orange(), second.orange()),
         operation.apply(first.magenta(), second.magenta()),
         operation.apply(first.lightBlue(), second.lightBlue()),
         operation.apply(first.yellow(), second.yellow()),
         operation.apply(first.lime(), second.lime()),
         operation.apply(first.pink(), second.pink()),
         operation.apply(first.gray(), second.gray()),
         operation.apply(first.lightGray(), second.lightGray()),
         operation.apply(first.cyan(), second.cyan()),
         operation.apply(first.purple(), second.purple()),
         operation.apply(first.blue(), second.blue()),
         operation.apply(first.brown(), second.brown()),
         operation.apply(first.green(), second.green()),
         operation.apply(first.red(), second.red()),
         operation.apply(first.black(), second.black())
      );
   }
}
