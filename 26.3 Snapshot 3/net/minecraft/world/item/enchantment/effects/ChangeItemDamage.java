package net.minecraft.world.item.enchantment.effects;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.minecraft.world.item.enchantment.LevelBasedValue;
import net.minecraft.world.phys.Vec3;

public record ChangeItemDamage(LevelBasedValue amount) implements EnchantmentEntityEffect {
   public static final MapCodec<ChangeItemDamage> CODEC = RecordCodecBuilder.mapCodec(
      i -> i.group(LevelBasedValue.CODEC.fieldOf("amount").forGetter(e -> e.amount)).apply(i, ChangeItemDamage::new)
   );

   @Override
   public void apply(final ServerLevel serverLevel, final int enchantmentLevel, final EnchantedItemInUse item, final Entity entity, final Vec3 position) {
      ItemStack itemStack = item.itemStack();
      if (itemStack.has(DataComponents.MAX_DAMAGE) && itemStack.has(DataComponents.DAMAGE)) {
         ServerPlayer player = item.owner() instanceof ServerPlayer sp ? sp : null;
         int change = (int)this.amount.calculate(enchantmentLevel);
         itemStack.hurtAndBreak(change, serverLevel, player, item.onBreak());
      }
   }

   @Override
   public MapCodec<ChangeItemDamage> codec() {
      return CODEC;
   }
}
