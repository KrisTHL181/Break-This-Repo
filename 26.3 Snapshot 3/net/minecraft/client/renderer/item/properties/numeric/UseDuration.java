package net.minecraft.client.renderer.item.properties.numeric;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

public record UseDuration(boolean remaining) implements RangeSelectItemModelProperty {
   public static final MapCodec<UseDuration> MAP_CODEC = RecordCodecBuilder.mapCodec(
      i -> i.group(Codec.BOOL.optionalFieldOf("remaining", false).forGetter(UseDuration::remaining)).apply(i, UseDuration::new)
   );

   @Override
   public float get(final ItemStack itemStack, final @Nullable ClientLevel level, final @Nullable ItemOwner owner, final int seed) {
      LivingEntity entity = owner == null ? null : owner.asLivingEntity();
      if (entity != null && entity.getUseItem() == itemStack) {
         return this.remaining ? entity.getUseItemRemainingTicks() : useDuration(itemStack, entity);
      } else {
         return 0.0F;
      }
   }

   @Override
   public MapCodec<UseDuration> type() {
      return MAP_CODEC;
   }

   public static int useDuration(final ItemStack itemStack, final LivingEntity owner) {
      return itemStack.getUseDuration(owner) - owner.getUseItemRemainingTicks();
   }
}
