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
      p_457057_ -> p_457057_.group(LevelBasedValue.CODEC.fieldOf("amount").forGetter(ApplyExhaustion::amount)).apply(p_457057_, ApplyExhaustion::new)
   );

   @Override
   public void apply(ServerLevel p_452260_, int p_461080_, EnchantedItemInUse p_459991_, Entity p_453772_, Vec3 p_452573_) {
      if (p_453772_ instanceof Player player) {
         player.causeFoodExhaustion(this.amount.calculate(p_461080_));
      }
   }

   @Override
   public MapCodec<ApplyExhaustion> codec() {
      return CODEC;
   }
}
