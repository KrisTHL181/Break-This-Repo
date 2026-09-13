package net.minecraft.client.renderer.item.properties.numeric;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

public record UseCycle(float period) implements RangeSelectItemModelProperty {
   public static final MapCodec<UseCycle> MAP_CODEC = RecordCodecBuilder.mapCodec(
      i -> i.group(ExtraCodecs.POSITIVE_FLOAT.optionalFieldOf("period", 1.0F).forGetter(UseCycle::period)).apply(i, UseCycle::new)
   );

   @Override
   public float get(final ItemStack itemStack, final @Nullable ClientLevel level, final @Nullable ItemOwner owner, final int seed) {
      LivingEntity entity = owner == null ? null : owner.asLivingEntity();
      return entity != null && entity.getUseItem() == itemStack ? entity.getUseItemRemainingTicks() % this.period : 0.0F;
   }

   @Override
   public MapCodec<UseCycle> type() {
      return MAP_CODEC;
   }
}
