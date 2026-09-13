package net.minecraft.client.renderer.item.properties.select;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

public record DisplayContext() implements SelectItemModelProperty<ItemDisplayContext> {
   public static final Codec<ItemDisplayContext> VALUE_CODEC = ItemDisplayContext.CODEC;
   public static final SelectItemModelProperty.Type<DisplayContext, ItemDisplayContext> TYPE = SelectItemModelProperty.Type.create(
      MapCodec.unit(new DisplayContext()), VALUE_CODEC
   );

   public ItemDisplayContext get(
      final ItemStack itemStack, final @Nullable ClientLevel level, final @Nullable LivingEntity owner, final int seed, final ItemDisplayContext displayContext
   ) {
      return displayContext;
   }

   @Override
   public SelectItemModelProperty.Type<DisplayContext, ItemDisplayContext> type() {
      return TYPE;
   }

   @Override
   public Codec<ItemDisplayContext> valueCodec() {
      return VALUE_CODEC;
   }
}
