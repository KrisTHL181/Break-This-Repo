package net.minecraft.client.renderer.item.properties.conditional;

import com.mojang.serialization.MapCodec;

public interface ConditionalItemModelProperty extends ItemModelPropertyTest {
   MapCodec<? extends ConditionalItemModelProperty> type();
}
