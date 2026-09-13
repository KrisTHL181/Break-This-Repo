package net.minecraft.client.color.item;

import com.mojang.serialization.MapCodec;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

public interface ItemTintSource {
   int calculate(ItemStack itemStack, @Nullable ClientLevel level, @Nullable LivingEntity owner);

   MapCodec<? extends ItemTintSource> type();
}
