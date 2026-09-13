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
      p_332691_ -> p_332691_.group(
            FireworkExplosion.Shape.CODEC.fieldOf("shape").forGetter(FireworkExplosion::shape),
            COLOR_LIST_CODEC.optionalFieldOf("colors", IntList.of()).forGetter(FireworkExplosion::colors),
            COLOR_LIST_CODEC.optionalFieldOf("fade_colors", IntList.of()).forGetter(FireworkExplosion::fadeColors),
            Codec.BOOL.optionalFieldOf("has_trail", false).forGetter(FireworkExplosion::hasTrail),
            Codec.BOOL.optionalFieldOf("has_twinkle", false).forGetter(FireworkExplosion::hasTwinkle)
         )
         .apply(p_332691_, FireworkExplosion::new)
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
   public void addToTooltip(Item.TooltipContext p_328877_, Consumer<Component> p_333224_, TooltipFlag p_335960_, DataComponentGetter p_395813_) {
      p_333224_.accept(this.shape.getName().withStyle(ChatFormatting.GRAY));
      this.addAdditionalTooltip(p_333224_);
   }

   public void addAdditionalTooltip(Consumer<Component> p_331797_) {
      if (!this.colors.isEmpty()) {
         p_331797_.accept(appendColors(Component.empty().withStyle(ChatFormatting.GRAY), this.colors));
      }

      if (!this.fadeColors.isEmpty()) {
         p_331797_.accept(
            appendColors(
               Component.translatable("item.minecraft.firework_star.fade_to").append(CommonComponents.SPACE).withStyle(ChatFormatting.GRAY), this.fadeColors
            )
         );
      }

      if (this.hasTrail) {
         p_331797_.accept(Component.translatable("item.minecraft.firework_star.trail").withStyle(ChatFormatting.GRAY));
      }

      if (this.hasTwinkle) {
         p_331797_.accept(Component.translatable("item.minecraft.firework_star.flicker").withStyle(ChatFormatting.GRAY));
      }
   }

   private static Component appendColors(MutableComponent p_333538_, IntList p_333652_) {
      for (int i = 0; i < p_333652_.size(); i++) {
         if (i > 0) {
            p_333538_.append(", ");
         }

         p_333538_.append(getColorName(p_333652_.getInt(i)));
      }

      return p_333538_;
   }

   private static Component getColorName(int p_333961_) {
      DyeColor dyecolor = DyeColor.byFireworkColor(p_333961_);
      return dyecolor == null ? CUSTOM_COLOR_NAME : Component.translatable("item.minecraft.firework_star." + dyecolor.getName());
   }

   public FireworkExplosion withFadeColors(IntList p_330299_) {
      return new FireworkExplosion(this.shape, this.colors, new IntArrayList(p_330299_), this.hasTrail, this.hasTwinkle);
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

      Shape(final int p_330815_, final String p_329574_) {
         this.id = p_330815_;
         this.name = p_329574_;
      }

      public MutableComponent getName() {
         return Component.translatable("item.minecraft.firework_star.shape." + this.name);
      }

      public int getId() {
         return this.id;
      }

      public static FireworkExplosion.Shape byId(int p_330413_) {
         return BY_ID.apply(p_330413_);
      }

      @Override
      public String getSerializedName() {
         return this.name;
      }
   }
}
