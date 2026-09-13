package net.minecraft.client.renderer.item.properties.select;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.trim.ArmorTrim;
import net.minecraft.world.item.equipment.trim.TrimMaterial;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public record TrimMaterialProperty() implements SelectItemModelProperty<ResourceKey<TrimMaterial>> {
   public static final Codec<ResourceKey<TrimMaterial>> VALUE_CODEC = ResourceKey.codec(Registries.TRIM_MATERIAL);
   public static final SelectItemModelProperty.Type<TrimMaterialProperty, ResourceKey<TrimMaterial>> TYPE = SelectItemModelProperty.Type.create(
      MapCodec.unit(new TrimMaterialProperty()), VALUE_CODEC
   );

   public @Nullable ResourceKey<TrimMaterial> get(
      ItemStack p_377294_, @Nullable ClientLevel p_377222_, @Nullable LivingEntity p_377672_, int p_378731_, ItemDisplayContext p_375810_
   ) {
      ArmorTrim armortrim = p_377294_.get(DataComponents.TRIM);
      return armortrim == null ? null : armortrim.material().unwrapKey().orElse(null);
   }

   @Override
   public SelectItemModelProperty.Type<TrimMaterialProperty, ResourceKey<TrimMaterial>> type() {
      return TYPE;
   }

   @Override
   public Codec<ResourceKey<TrimMaterial>> valueCodec() {
      return VALUE_CODEC;
   }
}
