package net.minecraft.client.renderer.item.properties.select;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

public record ContextEntityType() implements SelectItemModelProperty<ResourceKey<EntityType<?>>> {
   public static final Codec<ResourceKey<EntityType<?>>> VALUE_CODEC = ResourceKey.codec(Registries.ENTITY_TYPE);
   public static final SelectItemModelProperty.Type<ContextEntityType, ResourceKey<EntityType<?>>> TYPE = SelectItemModelProperty.Type.create(
      MapCodec.unit(new ContextEntityType()), VALUE_CODEC
   );

   public @Nullable ResourceKey<EntityType<?>> get(
      final ItemStack itemStack, final @Nullable ClientLevel level, final @Nullable LivingEntity owner, final int seed, final ItemDisplayContext displayContext
   ) {
      return owner == null ? null : owner.typeHolder().unwrapKey().orElse(null);
   }

   @Override
   public SelectItemModelProperty.Type<ContextEntityType, ResourceKey<EntityType<?>>> type() {
      return TYPE;
   }

   @Override
   public Codec<ResourceKey<EntityType<?>>> valueCodec() {
      return VALUE_CODEC;
   }
}
