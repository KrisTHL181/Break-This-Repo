package net.minecraft.client.color;

import com.google.common.collect.Maps;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.item.DyeColor;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ColorLerper {
   public static final DyeColor[] MUSIC_NOTE_COLORS = new DyeColor[]{
      DyeColor.WHITE,
      DyeColor.LIGHT_GRAY,
      DyeColor.LIGHT_BLUE,
      DyeColor.BLUE,
      DyeColor.CYAN,
      DyeColor.GREEN,
      DyeColor.LIME,
      DyeColor.YELLOW,
      DyeColor.ORANGE,
      DyeColor.PINK,
      DyeColor.RED,
      DyeColor.MAGENTA
   };

   public static int getLerpedColor(ColorLerper.Type p_406037_, float p_410004_) {
      int i = Mth.floor(p_410004_);
      int j = i / p_406037_.colorDuration;
      int k = p_406037_.colors.length;
      int l = j % k;
      int i1 = (j + 1) % k;
      float f = (i % p_406037_.colorDuration + Mth.frac(p_410004_)) / p_406037_.colorDuration;
      int j1 = p_406037_.getColor(p_406037_.colors[l]);
      int k1 = p_406037_.getColor(p_406037_.colors[i1]);
      return ARGB.srgbLerp(f, j1, k1);
   }

   static int getModifiedColor(DyeColor p_409905_, float p_407337_) {
      if (p_409905_ == DyeColor.WHITE) {
         return -1644826;
      }

      int i = p_409905_.getTextureDiffuseColor();
      return ARGB.color(255, Mth.floor(ARGB.red(i) * p_407337_), Mth.floor(ARGB.green(i) * p_407337_), Mth.floor(ARGB.blue(i) * p_407337_));
   }

   @OnlyIn(Dist.CLIENT)
   public enum Type {
      SHEEP(25, DyeColor.values(), 0.75F),
      MUSIC_NOTE(30, ColorLerper.MUSIC_NOTE_COLORS, 1.25F);

      final int colorDuration;
      private final Map<DyeColor, Integer> colorByDye;
      final DyeColor[] colors;

      Type(final int p_408068_, final DyeColor[] p_408212_, final float p_408392_) {
         this.colorDuration = p_408068_;
         this.colorByDye = Maps.newHashMap(
            Arrays.stream(p_408212_)
               .collect(Collectors.toMap(p_407631_ -> (DyeColor)p_407631_, p_407270_ -> ColorLerper.getModifiedColor(p_407270_, p_408392_)))
         );
         this.colors = p_408212_;
      }

      public final int getColor(DyeColor p_408467_) {
         return this.colorByDye.get(p_408467_);
      }
   }
}
