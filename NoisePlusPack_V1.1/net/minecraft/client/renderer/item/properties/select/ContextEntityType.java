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
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public record ContextEntityType() implements SelectItemModelProperty<ResourceKey<EntityType<?>>> {
   public static final Codec<ResourceKey<EntityType<?>>> VALUE_CODEC = ResourceKey.codec(Registries.ENTITY_TYPE);
   public static final SelectItemModelProperty.Type<ContextEntityType, ResourceKey<EntityType<?>>> TYPE = SelectItemModelProperty.Type.create(
      MapCodec.unit(new ContextEntityType()), VALUE_CODEC
   );

   public @Nullable ResourceKey<EntityType<?>> get(
      ItemStack p_377848_, @Nullable ClientLevel p_377744_, @Nullable LivingEntity p_377653_, int p_376783_, ItemDisplayContext p_377386_
   ) {
      return p_377653_ == null ? null : p_377653_.getType().builtInRegistryHolder().key();
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
