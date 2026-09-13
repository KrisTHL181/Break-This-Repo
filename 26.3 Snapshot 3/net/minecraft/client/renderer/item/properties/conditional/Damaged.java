package net.minecraft.client.renderer.item.properties.conditional;

import com.mojang.serialization.MapCodec;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

public record Damaged() implements ConditionalItemModelProperty {
   public static final MapCodec<Damaged> MAP_CODEC = MapCodec.unit(new Damaged());

   @Override
   public boolean get(
      final ItemStack itemStack, final @Nullable ClientLevel level, final @Nullable LivingEntity owner, final int seed, final ItemDisplayContext displayContext
   ) {
      return itemStack.isDamaged();
   }

   @Override
   public MapCodec<Damaged> type() {
      return MAP_CODEC;
   }
}
