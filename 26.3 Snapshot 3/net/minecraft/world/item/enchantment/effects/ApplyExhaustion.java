package net.minecraft.world.item.enchantment.effects;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.minecraft.world.item.enchantment.LevelBasedValue;
import net.minecraft.world.phys.Vec3;

public record ApplyExhaustion(LevelBasedValue amount) implements EnchantmentEntityEffect {
   public static final MapCodec<ApplyExhaustion> CODEC = RecordCodecBuilder.mapCodec(
      i -> i.group(LevelBasedValue.CODEC.fieldOf("amount").forGetter(ApplyExhaustion::amount)).apply(i, ApplyExhaustion::new)
   );

   @Override
   public void apply(final ServerLevel serverLevel, final int enchantmentLevel, final EnchantedItemInUse item, final Entity entity, final Vec3 position) {
      if (entity instanceof Player livingEntity) {
         livingEntity.causeFoodExhaustion(this.amount.calculate(enchantmentLevel));
      }
   }

   @Override
   public MapCodec<ApplyExhaustion> codec() {
      return CODEC;
   }
}
