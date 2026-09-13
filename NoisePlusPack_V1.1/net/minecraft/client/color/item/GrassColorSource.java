package net.minecraft.client.color.item;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GrassColor;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public record GrassColorSource(float temperature, float downfall) implements ItemTintSource {
   public static final MapCodec<GrassColorSource> MAP_CODEC = RecordCodecBuilder.mapCodec(
      p_376799_ -> p_376799_.group(
            ExtraCodecs.floatRange(0.0F, 1.0F).fieldOf("temperature").forGetter(GrassColorSource::temperature),
            ExtraCodecs.floatRange(0.0F, 1.0F).fieldOf("downfall").forGetter(GrassColorSource::downfall)
         )
         .apply(p_376799_, GrassColorSource::new)
   );

   public GrassColorSource() {
      this(0.5F, 1.0F);
   }

   @Override
   public int calculate(ItemStack p_378178_, @Nullable ClientLevel p_376564_, @Nullable LivingEntity p_378295_) {
      return GrassColor.get(this.temperature, this.downfall);
   }

   @Override
   public MapCodec<GrassColorSource> type() {
      return MAP_CODEC;
   }
}
