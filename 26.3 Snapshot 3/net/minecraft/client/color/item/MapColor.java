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
import org.jspecify.annotations.Nullable;

public record MapColor(int defaultColor) implements ItemTintSource {
   public static final MapCodec<MapColor> MAP_CODEC = RecordCodecBuilder.mapCodec(
      i -> i.group(ExtraCodecs.RGB_COLOR_CODEC.fieldOf("default").forGetter(MapColor::defaultColor)).apply(i, MapColor::new)
   );

   public MapColor() {
      this(MapItemColor.DEFAULT.rgb());
   }

   @Override
   public int calculate(final ItemStack itemStack, final @Nullable ClientLevel level, final @Nullable LivingEntity owner) {
      MapItemColor component = itemStack.get(DataComponents.MAP_COLOR);
      return component != null ? ARGB.opaque(component.rgb()) : ARGB.opaque(this.defaultColor);
   }

   @Override
   public MapCodec<MapColor> type() {
      return MAP_CODEC;
   }
}
