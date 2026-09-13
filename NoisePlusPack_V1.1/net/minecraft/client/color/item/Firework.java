package net.minecraft.client.color.item;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.ARGB;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.FireworkExplosion;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public record Firework(int defaultColor) implements ItemTintSource {
   public static final MapCodec<Firework> MAP_CODEC = RecordCodecBuilder.mapCodec(
      p_375678_ -> p_375678_.group(ExtraCodecs.RGB_COLOR_CODEC.fieldOf("default").forGetter(Firework::defaultColor)).apply(p_375678_, Firework::new)
   );

   public Firework() {
      this(-7697782);
   }

   @Override
   public int calculate(ItemStack p_378616_, @Nullable ClientLevel p_375586_, @Nullable LivingEntity p_376697_) {
      FireworkExplosion fireworkexplosion = p_378616_.get(DataComponents.FIREWORK_EXPLOSION);
      IntList intlist = fireworkexplosion != null ? fireworkexplosion.colors() : IntList.of();
      int i = intlist.size();
      if (i == 0) {
         return this.defaultColor;
      }

      if (i == 1) {
         return ARGB.opaque(intlist.getInt(0));
      }

      int j = 0;
      int k = 0;
      int l = 0;

      for (int i1 = 0; i1 < i; i1++) {
         int j1 = intlist.getInt(i1);
         j += ARGB.red(j1);
         k += ARGB.green(j1);
         l += ARGB.blue(j1);
      }

      return ARGB.color(j / i, k / i, l / i);
   }

   @Override
   public MapCodec<Firework> type() {
      return MAP_CODEC;
   }
}
