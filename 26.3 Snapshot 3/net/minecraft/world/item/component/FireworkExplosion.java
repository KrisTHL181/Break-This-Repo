package net.minecraft.world.item.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import java.util.ArrayList;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;

public record FireworkExplosion(FireworkExplosion.Shape shape, IntList colors, IntList fadeColors, boolean hasTrail, boolean hasTwinkle)
   implements TooltipProvider {
   public static final FireworkExplosion DEFAULT = new FireworkExplosion(FireworkExplosion.Shape.SMALL_BALL, IntList.of(), IntList.of(), false, false);
   public static final Codec<IntList> COLOR_LIST_CODEC = Codec.INT.listOf().xmap(IntArrayList::new, ArrayList::new);
   public static final Codec<FireworkExplosion> CODEC = RecordCodecBuilder.create(
      i -> i.group(
            FireworkExplosion.Shape.CODEC.fieldOf("shape").forGetter(FireworkExplosion::shape),
            COLOR_LIST_CODEC.optionalFieldOf("colors", IntList.of()).forGetter(FireworkExplosion::colors),
            COLOR_LIST_CODEC.optionalFieldOf("fade_colors", IntList.of()).forGetter(FireworkExplosion::fadeColors),
            Codec.BOOL.optionalFieldOf("has_trail", false).forGetter(FireworkExplosion::hasTrail),
            Codec.BOOL.optionalFieldOf("has_twinkle", false).forGetter(FireworkExplosion::hasTwinkle)
         )
         .apply(i, FireworkExplosion::new)
   );
   private static final StreamCodec<ByteBuf, IntList> COLOR_LIST_STREAM_CODEC = ByteBufCodecs.INT
      .apply(ByteBufCodecs.list())
      .map(IntArrayList::new, ArrayList::new);
   public static final StreamCodec<ByteBuf, FireworkExplosion> STREAM_CODEC = StreamCodec.composite(
      FireworkExplosion.Shape.STREAM_CODEC,
      FireworkExplosion::shape,
      COLOR_LIST_STREAM_CODEC,
      FireworkExplosion::colors,
      COLOR_LIST_STREAM_CODEC,
      FireworkExplosion::fadeColors,
      ByteBufCodecs.BOOL,
      FireworkExplosion::hasTrail,
      ByteBufCodecs.BOOL,
      FireworkExplosion::hasTwinkle,
      FireworkExplosion::new
   );
   private static final Component CUSTOM_COLOR_NAME = Component.translatable("item.minecraft.firework_star.custom_color");

   @Override
   public void addToTooltip(final Item.TooltipContext context, final Consumer<Component> consumer, final TooltipFlag flag, final DataComponentGetter components) {
      consumer.accept(this.shape.getName().withStyle(ChatFormatting.GRAY));
      this.addAdditionalTooltip(consumer);
   }

   public void addAdditionalTooltip(final Consumer<Component> consumer) {
      if (!this.colors.isEmpty()) {
         consumer.accept(appendColors(Component.empty().withStyle(ChatFormatting.GRAY), this.colors));
      }

      if (!this.fadeColors.isEmpty()) {
         consumer.accept(
            appendColors(
               Component.translatable("item.minecraft.firework_star.fade_to").append(CommonComponents.SPACE).withStyle(ChatFormatting.GRAY), this.fadeColors
            )
         );
      }

      if (this.hasTrail) {
         consumer.accept(Component.translatable("item.minecraft.firework_star.trail").withStyle(ChatFormatting.GRAY));
      }

      if (this.hasTwinkle) {
         consumer.accept(Component.translatable("item.minecraft.firework_star.flicker").withStyle(ChatFormatting.GRAY));
      }
   }

   private static Component appendColors(final MutableComponent builder, final IntList colors) {
      for (int i = 0; i < colors.size(); i++) {
         if (i > 0) {
            builder.append(", ");
         }

         builder.append(getColorName(colors.getInt(i)));
      }

      return builder;
   }

   private static Component getColorName(final int colorIndex) {
      DyeColor color = DyeColor.byFireworkColor(colorIndex);
      return color == null ? CUSTOM_COLOR_NAME : Component.translatable("item.minecraft.firework_star." + color.getName());
   }

   public FireworkExplosion withFadeColors(final IntList fadeColors) {
      return new FireworkExplosion(this.shape, this.colors, new IntArrayList(fadeColors), this.hasTrail, this.hasTwinkle);
   }

   public enum Shape implements StringRepresentable {
      SMALL_BALL(0, "small_ball"),
      LARGE_BALL(1, "large_ball"),
      STAR(2, "star"),
      CREEPER(3, "creeper"),
      BURST(4, "burst");

      private static final IntFunction<FireworkExplosion.Shape> BY_ID = ByIdMap.continuous(
         FireworkExplosion.Shape::getId, values(), ByIdMap.OutOfBoundsStrategy.ZERO
      );
      public static final StreamCodec<ByteBuf, FireworkExplosion.Shape> STREAM_CODEC = ByteBufCodecs.idMapper(BY_ID, FireworkExplosion.Shape::getId);
      public static final Codec<FireworkExplosion.Shape> CODEC = StringRepresentable.fromValues(FireworkExplosion.Shape::values);
      private final int id;
      private final String name;

      Shape(final int id, final String name) {
         this.id = id;
         this.name = name;
      }

      public MutableComponent getName() {
         return Component.translatable("item.minecraft.firework_star.shape." + this.name);
      }

      public int getId() {
         return this.id;
      }

      public static FireworkExplosion.Shape byId(final int id) {
         return BY_ID.apply(id);
      }

      @Override
      public String getSerializedName() {
         return this.name;
      }
   }
}
