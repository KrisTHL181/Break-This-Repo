package net.minecraft.client.color.item;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public record Dye(int defaultColor) implements ItemTintSource {
   public static final MapCodec<Dye> MAP_CODEC = RecordCodecBuilder.mapCodec(
      p_377020_ -> p_377020_.group(ExtraCodecs.RGB_COLOR_CODEC.fieldOf("default").forGetter(Dye::defaultColor)).apply(p_377020_, Dye::new)
   );

   @Override
   public int calculate(ItemStack p_377608_, @Nullable ClientLevel p_377889_, @Nullable LivingEntity p_378264_) {
      return DyedItemColor.getOrDefault(p_377608_, this.defaultColor);
   }

   @Override
   public MapCodec<Dye> type() {
      return MAP_CODEC;
   }
}
