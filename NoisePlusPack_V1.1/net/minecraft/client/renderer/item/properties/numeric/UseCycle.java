package net.minecraft.client.renderer.item.properties.numeric;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public record UseCycle(float period) implements RangeSelectItemModelProperty {
   public static final MapCodec<UseCycle> MAP_CODEC = RecordCodecBuilder.mapCodec(
      p_375755_ -> p_375755_.group(ExtraCodecs.POSITIVE_FLOAT.optionalFieldOf("period", 1.0F).forGetter(UseCycle::period)).apply(p_375755_, UseCycle::new)
   );

   @Override
   public float get(ItemStack p_377041_, @Nullable ClientLevel p_378065_, @Nullable ItemOwner p_423815_, int p_376704_) {
      LivingEntity livingentity = p_423815_ == null ? null : p_423815_.asLivingEntity();
      return livingentity != null && livingentity.getUseItem() == p_377041_ ? livingentity.getUseItemRemainingTicks() % this.period : 0.0F;
   }

   @Override
   public MapCodec<UseCycle> type() {
      return MAP_CODEC;
   }
}
