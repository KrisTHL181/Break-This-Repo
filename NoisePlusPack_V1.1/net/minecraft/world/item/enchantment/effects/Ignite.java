package net.minecraft.world.item.enchantment.effects;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.minecraft.world.item.enchantment.LevelBasedValue;
import net.minecraft.world.phys.Vec3;

public record Ignite(LevelBasedValue duration) implements EnchantmentEntityEffect {
   public static final MapCodec<Ignite> CODEC = RecordCodecBuilder.mapCodec(
      p_342125_ -> p_342125_.group(LevelBasedValue.CODEC.fieldOf("duration").forGetter(p_344657_ -> p_344657_.duration)).apply(p_342125_, Ignite::new)
   );

   @Override
   public void apply(ServerLevel p_343819_, int p_342380_, EnchantedItemInUse p_343404_, Entity p_345145_, Vec3 p_344350_) {
      p_345145_.igniteForSeconds(this.duration.calculate(p_342380_));
   }

   @Override
   public MapCodec<Ignite> codec() {
      return CODEC;
   }
}
