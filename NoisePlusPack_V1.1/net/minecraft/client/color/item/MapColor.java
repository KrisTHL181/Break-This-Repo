package net.minecraft.client.color.item;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.ARGB;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.MapItemColor;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public record MapColor(int defaultColor) implements ItemTintSource {
   public static final MapCodec<MapColor> MAP_CODEC = RecordCodecBuilder.mapCodec(
      p_377101_ -> p_377101_.group(ExtraCodecs.RGB_COLOR_CODEC.fieldOf("default").forGetter(MapColor::defaultColor)).apply(p_377101_, MapColor::new)
   );

   public MapColor() {
      this(MapItemColor.DEFAULT.rgb());
   }

   @Override
   public int calculate(ItemStack p_378190_, @Nullable ClientLevel p_375429_, @Nullable LivingEntity p_377293_) {
      MapItemColor mapitemcolor = p_378190_.get(DataComponents.MAP_COLOR);
      return mapitemcolor != null ? ARGB.opaque(mapitemcolor.rgb()) : ARGB.opaque(this.defaultColor);
   }

   @Override
   public MapCodec<MapColor> type() {
      return MAP_CODEC;
   }
}
