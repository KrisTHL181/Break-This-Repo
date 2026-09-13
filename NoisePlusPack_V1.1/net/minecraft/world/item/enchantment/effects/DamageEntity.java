package net.minecraft.world.item.enchantment.effects;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.minecraft.world.item.enchantment.LevelBasedValue;
import net.minecraft.world.phys.Vec3;

public record DamageEntity(LevelBasedValue minDamage, LevelBasedValue maxDamage, Holder<DamageType> damageType) implements EnchantmentEntityEffect {
   public static final MapCodec<DamageEntity> CODEC = RecordCodecBuilder.mapCodec(
      p_342476_ -> p_342476_.group(
            LevelBasedValue.CODEC.fieldOf("min_damage").forGetter(DamageEntity::minDamage),
            LevelBasedValue.CODEC.fieldOf("max_damage").forGetter(DamageEntity::maxDamage),
            DamageType.CODEC.fieldOf("damage_type").forGetter(DamageEntity::damageType)
         )
         .apply(p_342476_, DamageEntity::new)
   );

   @Override
   public void apply(ServerLevel p_344348_, int p_344737_, EnchantedItemInUse p_343017_, Entity p_343629_, Vec3 p_342181_) {
      float f = Mth.randomBetween(p_343629_.getRandom(), this.minDamage.calculate(p_344737_), this.maxDamage.calculate(p_344737_));
      p_343629_.hurtServer(p_344348_, new DamageSource(this.damageType, p_343017_.owner()), f);
   }

   @Override
   public MapCodec<DamageEntity> codec() {
      return CODEC;
   }
}
