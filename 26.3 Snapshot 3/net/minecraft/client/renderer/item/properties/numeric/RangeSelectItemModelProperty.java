package net.minecraft.client.renderer.item.properties.numeric;

import com.mojang.serialization.MapCodec;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

public interface RangeSelectItemModelProperty {
   float get(ItemStack itemStack, @Nullable ClientLevel level, @Nullable ItemOwner owner, int seed);

   MapCodec<? extends RangeSelectItemModelProperty> type();
}
