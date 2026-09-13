package net.minecraft.world.item.enchantment.effects;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.function.Function;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.minecraft.world.phys.Vec3;

public interface AllOf {
   static <T, A extends T> MapCodec<A> codec(Codec<T> p_344213_, Function<List<T>, A> p_343453_, Function<A, List<T>> p_344873_) {
      return RecordCodecBuilder.mapCodec(p_344396_ -> p_344396_.group(p_344213_.listOf().fieldOf("effects").forGetter(p_344873_)).apply(p_344396_, p_343453_));
   }

   static AllOf.EntityEffects entityEffects(EnchantmentEntityEffect... p_345218_) {
      return new AllOf.EntityEffects(List.of(p_345218_));
   }

   static AllOf.LocationBasedEffects locationBasedEffects(EnchantmentLocationBasedEffect... p_343547_) {
      return new AllOf.LocationBasedEffects(List.of(p_343547_));
   }

   static AllOf.ValueEffects valueEffects(EnchantmentValueEffect... p_344769_) {
      return new AllOf.ValueEffects(List.of(p_344769_));
   }

   record EntityEffects(List<EnchantmentEntityEffect> effects) implements EnchantmentEntityEffect {
      public static final MapCodec<AllOf.EntityEffects> CODEC = AllOf.codec(
         EnchantmentEntityEffect.CODEC, AllOf.EntityEffects::new, AllOf.EntityEffects::effects
      );

      @Override
      public void apply(ServerLevel p_343230_, int p_344735_, EnchantedItemInUse p_345167_, Entity p_342541_, Vec3 p_344713_) {
         for (EnchantmentEntityEffect enchantmententityeffect : this.effects) {
            enchantmententityeffect.apply(p_343230_, p_344735_, p_345167_, p_342541_, p_344713_);
         }
      }

      @Override
      public MapCodec<AllOf.EntityEffects> codec() {
         return CODEC;
      }
   }

   record LocationBasedEffects(List<EnchantmentLocationBasedEffect> effects) implements EnchantmentLocationBasedEffect {
      public static final MapCodec<AllOf.LocationBasedEffects> CODEC = AllOf.codec(
         EnchantmentLocationBasedEffect.CODEC, AllOf.LocationBasedEffects::new, AllOf.LocationBasedEffects::effects
      );

      @Override
      public void onChangedBlock(ServerLevel p_344697_, int p_345161_, EnchantedItemInUse p_342856_, Entity p_342868_, Vec3 p_344021_, boolean p_342990_) {
         for (EnchantmentLocationBasedEffect enchantmentlocationbasedeffect : this.effects) {
            enchantmentlocationbasedeffect.onChangedBlock(p_344697_, p_345161_, p_342856_, p_342868_, p_344021_, p_342990_);
         }
      }

      @Override
      public void onDeactivated(EnchantedItemInUse p_344276_, Entity p_343864_, Vec3 p_344250_, int p_342496_) {
         for (EnchantmentLocationBasedEffect enchantmentlocationbasedeffect : this.effects) {
            enchantmentlocationbasedeffect.onDeactivated(p_344276_, p_343864_, p_344250_, p_342496_);
         }
      }

      @Override
      public MapCodec<AllOf.LocationBasedEffects> codec() {
         return CODEC;
      }
   }

   record ValueEffects(List<EnchantmentValueEffect> effects) implements EnchantmentValueEffect {
      public static final MapCodec<AllOf.ValueEffects> CODEC = AllOf.codec(EnchantmentValueEffect.CODEC, AllOf.ValueEffects::new, AllOf.ValueEffects::effects);

      @Override
      public float process(int p_343759_, RandomSource p_342776_, float p_344071_) {
         for (EnchantmentValueEffect enchantmentvalueeffect : this.effects) {
            p_344071_ = enchantmentvalueeffect.process(p_343759_, p_342776_, p_344071_);
         }

         return p_344071_;
      }

      @Override
      public MapCodec<AllOf.ValueEffects> codec() {
         return CODEC;
      }
   }
}
