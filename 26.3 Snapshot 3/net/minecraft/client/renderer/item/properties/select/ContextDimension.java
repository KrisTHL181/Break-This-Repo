package net.minecraft.client.renderer.item.properties.select;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

public record ContextDimension() implements SelectItemModelProperty<ResourceKey<Level>> {
   public static final Codec<ResourceKey<Level>> VALUE_CODEC = ResourceKey.codec(Registries.DIMENSION);
   public static final SelectItemModelProperty.Type<ContextDimension, ResourceKey<Level>> TYPE = SelectItemModelProperty.Type.create(
      MapCodec.unit(new ContextDimension()), VALUE_CODEC
   );

   public @Nullable ResourceKey<Level> get(
      final ItemStack itemStack, final @Nullable ClientLevel level, final @Nullable LivingEntity owner, final int seed, final ItemDisplayContext displayContext
   ) {
      return level != null ? level.dimension() : null;
   }

   @Override
   public SelectItemModelProperty.Type<ContextDimension, ResourceKey<Level>> type() {
      return TYPE;
   }

   @Override
   public Codec<ResourceKey<Level>> valueCodec() {
      return VALUE_CODEC;
   }
}
