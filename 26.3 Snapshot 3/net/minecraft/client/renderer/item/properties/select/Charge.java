package net.minecraft.client.renderer.item.properties.select;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ChargedProjectiles;
import org.jspecify.annotations.Nullable;

public record Charge() implements SelectItemModelProperty<CrossbowItem.ChargeType> {
   public static final Codec<CrossbowItem.ChargeType> VALUE_CODEC = CrossbowItem.ChargeType.CODEC;
   public static final SelectItemModelProperty.Type<Charge, CrossbowItem.ChargeType> TYPE = SelectItemModelProperty.Type.create(
      MapCodec.unit(new Charge()), VALUE_CODEC
   );

   public CrossbowItem.ChargeType get(
      final ItemStack itemStack, final @Nullable ClientLevel level, final @Nullable LivingEntity owner, final int seed, final ItemDisplayContext displayContext
   ) {
      ChargedProjectiles projectiles = itemStack.get(DataComponents.CHARGED_PROJECTILES);
      if (projectiles == null || projectiles.isEmpty()) {
         return CrossbowItem.ChargeType.NONE;
      } else {
         return projectiles.contains(Items.FIREWORK_ROCKET) ? CrossbowItem.ChargeType.ROCKET : CrossbowItem.ChargeType.ARROW;
      }
   }

   @Override
   public SelectItemModelProperty.Type<Charge, CrossbowItem.ChargeType> type() {
      return TYPE;
   }

   @Override
   public Codec<CrossbowItem.ChargeType> valueCodec() {
      return VALUE_CODEC;
   }
}
